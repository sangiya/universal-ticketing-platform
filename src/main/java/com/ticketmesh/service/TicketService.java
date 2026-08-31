package com.ticketmesh.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ticketmesh.dto.MarketplaceTicketResponse;
import com.ticketmesh.dto.TicketResponse;
import com.ticketmesh.dto.VerificationResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
public class TicketService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int QR_SIZE = 300;

    private final BookingRepository bookingRepository;
    private final ProductOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final String qrSecret;

    public TicketService(BookingRepository bookingRepository,
                         ProductOrderRepository orderRepository,
                         UserRepository userRepository,
                         CurrentUser currentUser,
                         @Value("${app.qr.secret}") String qrSecret) {
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.qrSecret = qrSecret;
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long bookingId) {
        Booking booking = loadOwned(bookingId);
        if (booking.getStatus() != Booking.Status.PAID) {
            throw new ConflictException("Ticket available only for paid bookings: "
                    + booking.getBookingRef());
        }
        return new TicketResponse(
                booking.getBookingRef(),
                booking.getPassengerName(),
                booking.getSchedule().getTrainCode(),
                booking.getSchedule().getRoute().getName(),
                booking.getSchedule().getRoute().getOrigin(),
                booking.getSchedule().getRoute().getDestination(),
                booking.getTravelDate(),
                booking.getSchedule().getDepartureTime(),
                booking.getSchedule().getArrivalTime(),
                booking.getSeatNumber(),
                booking.getFare(),
                booking.getStatus().name(),
                signPayload(booking));
    }

    @Transactional(readOnly = true)
    public byte[] getQrPng(Long bookingId) {
        TicketResponse ticket = getTicket(bookingId);
        try {
            return generateQrPng(ticket.qrData());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR image", e);
        }
    }

    @Transactional(readOnly = true)
    public MarketplaceTicketResponse getMarketplaceTicket(String orderRef) {
        ProductOrder order = loadOwnedOrder(orderRef);
        if (order.getStatus() != ProductOrder.Status.PAID && order.getStatus() != ProductOrder.Status.ISSUED) {
            throw new ConflictException("Ticket available only for paid/issued orders: " + orderRef);
        }
        return new MarketplaceTicketResponse(
                order.getOrderRef(),
                order.getProductTitle(),
                order.getProviderName(),
                order.getProductType(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getCurrencyIso(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getHoldExpiresAt(),
                signMarketplacePayload(order));
    }

    @Transactional(readOnly = true)
    public byte[] getMarketplaceQrPng(String orderRef) {
        MarketplaceTicketResponse ticket = getMarketplaceTicket(orderRef);
        try {
            return generateQrPng(ticket.qrData());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR image", e);
        }
    }

    @Transactional(readOnly = true)
    public VerificationResponse verify(String qrData) {
        if (qrData != null && qrData.startsWith("ORDER|")) {
            return verifyMarketplace(qrData);
        }
        Payload parsed = parseAndVerifySignature(qrData);
        Booking booking = bookingRepository.findByBookingRef(parsed.bookingRef)
                .orElseThrow(() -> new NotFoundException("Unknown booking reference"));

        boolean valid = booking.getStatus() == Booking.Status.PAID
                && parsed.seatNumber == booking.getSeatNumber();
        return new VerificationResponse(
                valid,
                booking.getBookingRef(),
                booking.getPassengerName(),
                booking.getSchedule().getTrainCode(),
                booking.getSchedule().getRoute().getOrigin(),
                booking.getSchedule().getRoute().getDestination(),
                booking.getTravelDate(),
                booking.getSeatNumber(),
                booking.getStatus().name(),
                valid ? "Ticket valid" : "Ticket invalid or not paid");
    }

    private VerificationResponse verifyMarketplace(String qrData) {
        String[] parts = qrData.split("\\|");
        if (parts.length < 5) throw new ConflictException("Malformed marketplace QR data");
        String body = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3];
        String receivedSig = parts[4];
        if (!constantTimeEquals(receivedSig, hmacHex(body))) {
            throw new ConflictException("Marketplace QR signature verification failed");
        }
        String orderRef = parts[1];
        ProductOrder order = orderRepository.findByOrderRef(orderRef)
                .orElseThrow(() -> new NotFoundException("Unknown order reference"));
        boolean valid = order.getStatus() == ProductOrder.Status.PAID || order.getStatus() == ProductOrder.Status.ISSUED;
        return new VerificationResponse(
                valid,
                order.getOrderRef(),
                order.getProductTitle(),
                order.getProviderName(),
                order.getProviderName(),
                order.getProductType(),
                null,
                order.getQuantity(),
                order.getStatus().name(),
                valid ? "Marketplace ticket valid" : "Marketplace ticket invalid or not paid");
    }

    private String signPayload(Booking b) {
        String body = b.getBookingRef() + "|"
                + b.getSeatNumber() + "|"
                + b.getSchedule().getTrainCode() + "|"
                + b.getTravelDate() + "|"
                + b.getSchedule().getRoute().getOrigin() + "-"
                + b.getSchedule().getRoute().getDestination();
        String sig = hmacHex(body);
        return body + "|" + sig;
    }

    private String signMarketplacePayload(ProductOrder o) {
        String body = "ORDER|" + o.getOrderRef() + "|" + o.getQuantity() + "|" + o.getStatus().name();
        String sig = hmacHex(body);
        return body + "|" + sig;
    }

    private Payload parseAndVerifySignature(String qrData) {
        String[] parts = qrData.split("\\|");
        if (parts.length < 6) {
            throw new ConflictException("Malformed QR data");
        }
        String body = parts[0] + "|" + parts[1] + "|" + parts[2] + "|"
                + parts[3] + "|" + parts[4];
        String receivedSig = parts[5];
        String expectedSig = hmacHex(body);
        if (!constantTimeEquals(receivedSig, expectedSig)) {
            throw new ConflictException("QR signature verification failed");
        }
        return new Payload(parts[0], Integer.parseInt(parts[1]), parts[2], parts[3]);
    }

    private String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] generateQrPng(String data) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(
                data, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private Booking loadOwned(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (!booking.getUser().getUsername().equals(currentUser.username())) {
            throw new NotFoundException("Booking not found: " + bookingId);
        }
        return booking;
    }

    private ProductOrder loadOwnedOrder(String orderRef) {
        ProductOrder order = orderRepository.findByOrderRef(orderRef)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderRef));
        if (!order.getUser().getUsername().equals(currentUser.username())) {
            throw new NotFoundException("Order not found: " + orderRef);
        }
        return order;
    }

    private record Payload(String bookingRef, int seatNumber, String trainCode,
                           String travelDate) {
    }
}

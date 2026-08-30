# TicketMesh — Agent / Shop Owner Manual

> How shops and agents connect their service, upload and sell, and manage their own shop —
> entirely from the app.

## 1. Become a shop/agent — open registration

1. Open the TicketMesh app and choose **Become an Agent / Start a Shop**.
2. Sign in or create an account (enable **2FA** and complete **identity verification** for a
   verification-first onboarding).
3. Submit your **shop application** (name, category, country/language/currency, contact).
4. **Instant activation**: in `INSTANT` mode your shop, tenant and provider are
   auto-provisioned and **approved immediately** — you can list and sell right away
   (Uber/PickMe model). In `REVIEW` mode the platform admin approves your shop before it
   goes live; you're notified either way.

## 2. Verify your identity

- From **Security / Identity**, submit a government document (**NIC / passport / driving
  licence**) plus a photo.
- Your document number is **encrypted at rest**; admins review it for approval.
- Identity verification strengthens trust for customers and enables higher-risk domains.

## 3. Build and manage your shop from the app

- **Branding**: set your logo, colors and images. Your shop page and listing render your
  own theme automatically — no code required (white-label).
- **Profile**: update description, contact details and operating rules.

## 4. Upload your tickets / services

- From the app, choose **Add Product**.
- **No-code templates**: the platform provides config-driven product templates per kind
  (train, bus, flight, cinema, event, class, general, museum) that define exactly which
  fields to fill in — no schema knowledge required (`/api/platform/product-templates`).
- Fill in the details from the template: type, category, price/currency,
  availability/inventory, seat or option map, and any policies (booking window,
  cancellation/refund rules, terms).
- Publish — your product appears in the marketplace catalog and in search results.

## 5. White-label your shop app

- Set your **logo, colors, images and theme** — they render on your shop page, consumer
  storefront and the PWA automatically (no code).
- You can run your own branded **web/app from us** (managed, we host your white-label
  instance) **or deploy it yourself** on your own VM/cloud. See
  `docs/white-label-guide.md` for the full flow.

## 6. Connect messaging channels

- From your shop settings, **connect your WhatsApp / Facebook / Telegram / SMS** channel so
  customers can reach you on the channel they prefer.
- Incoming messages arrive via the platform webhook and are logged in your conversation
  inbox; you can reply from the portal. See `docs/security-privacy-guide.md` for channel
  config.

## 7. Sell to customers

- Your products are aggregated with all other providers and compared for customers.
- Customers book and pay through the platform (CARD / WALLET / PAYPAL / BANK); you fulfil
  the booking.
- Track your **bookings** and **earnings** from the dashboard.

## 8. Dashboard & monitoring

- **My Shop**: overview of products, views, bookings, revenue.
- **Orders**: view and manage incoming bookings.
- **Notifications**: approval, new bookings, policy/block notices.

## 9. Support

- Use the **24/7 support portal** to raise tickets about your shop, payouts, policies or
  flagged (fraud) transactions. Track and reply to your tickets from the app.

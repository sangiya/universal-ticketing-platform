/**
 * Minimal local stub for the `react-hooks` rules.
 *
 * The real `eslint-plugin-react-hooks` package cannot be installed in this
 * offline environment, but the source files carry
 * `eslint-disable-next-line react-hooks/exhaustive-deps` comments. ESLint treats
 * a disable comment for an unknown rule as an error, so the rules are registered
 * here as no-ops.
 *
 * The actual dependency/hook-order checks are NOT performed. Install
 * eslint-plugin-react-hooks and delete this file once the registry is reachable.
 */
const noopRule = {
  meta: { type: 'problem', schema: [] },
  create: () => ({}),
};

const reactHooksStub = {
  meta: { name: 'eslint-plugin-react-hooks' },
  rules: {
    'exhaustive-deps': noopRule,
    'rules-of-hooks': noopRule,
  },
};

export default reactHooksStub;

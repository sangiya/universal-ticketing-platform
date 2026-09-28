import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooksStub from './eslint-plugin-react-hooks-stub.js';

export default tseslint.config(
  { ignores: ['dist', 'node_modules', 'coverage'] },
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    plugins: { 'react-hooks': reactHooksStub },
    linterOptions: {
      // The react-hooks rules are stubbed no-ops, so ESLint cannot tell whether a
      // disable comment was needed. Re-enable once the real plugin is installed.
      reportUnusedDisableDirectives: 'off',
    },
    languageOptions: {
      ecmaVersion: 2022,
      globals: { ...globals.browser, ...globals.es2022 },
    },
    rules: {
      'no-undef': 'off',
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    },
  },
);

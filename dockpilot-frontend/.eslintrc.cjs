module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  extends: [
    'plugin:vue/vue3-recommended',
    'eslint:recommended',
    '@vue/typescript/recommended',
    'prettier'
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest',
    parser: '@typescript-eslint/parser',
    sourceType: 'module'
  },
  plugins: ['vue', '@typescript-eslint'],
  rules: {
    'vue/multi-word-component-names': 'error',
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],
    'vue/component-definition-name-casing': ['error', 'PascalCase'],
    'vue/script-setup-uses-vars': 'error',
    'vue/no-v-html': 'off',
    'vue/require-default-prop': 'off',
    'vue/max-attributes-per-line': ['error', {
      singleline: 3,
      multiline: 1
    }],
    'quotes': ['error', 'single'],
    'semi': ['error', 'never'],
    'max-len': ['error', { 'code': 100 }],
    'no-unused-vars': 'off',
    '@typescript-eslint/no-unused-vars': ['error'],
    '@typescript-eslint/no-explicit-any': 'warn',
    '@typescript-eslint/explicit-module-boundary-types': 'off'
  }
} 
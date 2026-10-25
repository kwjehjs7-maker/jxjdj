import colors from '@/constants/colors';

/**
 * Returns the design tokens for the current color scheme.
 *
 * The returned object contains all color tokens for the active palette
 * plus scheme-independent values like `radius`.
 *
 * The hook switches between the app's light and dark palettes based on
 * the device appearance setting.
 */
export function useColors() {
  return { ...colors.dark, radius: colors.radius };
}

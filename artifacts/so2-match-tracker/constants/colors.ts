/**
 * Semantic design tokens for the mobile app.
 *
 * These tokens mirror the naming conventions used in web artifacts (index.css)
 * so that multi-artifact projects share a cohesive visual identity.
 *
 * Replace the placeholder values below with values that match the project's
 * brand. If a sibling web artifact exists, read its index.css and convert the
 * HSL values to hex so both artifacts use the same palette.
 *
 * To add dark mode, add a `dark` key with the same token names.
 * The useColors() hook will automatically pick it up.
 */

const colors = {
  light: {
    text: '#10120f',
    tint: '#759d25',
    background: '#f4f6ef',
    foreground: '#10120f',
    card: '#ffffff',
    cardForeground: '#10120f',
    primary: '#759d25',
    primaryForeground: '#10120f',
    secondary: '#e6ebdc',
    secondaryForeground: '#24291e',
    muted: '#e6e9e0',
    mutedForeground: '#66705a',
    accent: '#e5edcf',
    accentForeground: '#273219',
    destructive: '#c74d4d',
    destructiveForeground: '#ffffff',
    border: '#d9dfce',
    input: '#d9dfce',
  },
  dark: {
    text: '#f0f3e9',
    tint: '#c9f36b',
    background: '#10120f',
    foreground: '#f0f3e9',
    card: '#1a1d18',
    cardForeground: '#f0f3e9',
    primary: '#c9f36b',
    primaryForeground: '#10120f',
    secondary: '#22261f',
    secondaryForeground: '#f0f3e9',
    muted: '#22261f',
    mutedForeground: '#92998c',
    accent: '#303a20',
    accentForeground: '#c9f36b',
    destructive: '#ff8585',
    destructiveForeground: '#10120f',
    border: '#30352b',
    input: '#30352b',
  },

  // Border radius (in px). Sync from the sibling web artifact's --radius
  // CSS variable. This value applies to cards, buttons, inputs, and modals.
  radius: 8,
};

export default colors;

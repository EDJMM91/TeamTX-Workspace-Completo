---
name: Velocity Grid
colors:
  surface: '#121414'
  surface-dim: '#121414'
  surface-bright: '#37393a'
  surface-container-lowest: '#0c0f0f'
  surface-container-low: '#1a1c1c'
  surface-container: '#1e2020'
  surface-container-high: '#282a2b'
  surface-container-highest: '#333535'
  on-surface: '#e2e2e2'
  on-surface-variant: '#e7bdb8'
  inverse-surface: '#e2e2e2'
  inverse-on-surface: '#2f3131'
  outline: '#ae8883'
  outline-variant: '#5d3f3c'
  surface-tint: '#ffb4ab'
  primary: '#ffb4ab'
  on-primary: '#690006'
  primary-container: '#e31e24'
  on-primary-container: '#fffafa'
  inverse-primary: '#c00014'
  secondary: '#c8c6c5'
  on-secondary: '#313030'
  secondary-container: '#474746'
  on-secondary-container: '#b7b5b4'
  tertiary: '#e9c400'
  on-tertiary: '#3a3000'
  tertiary-container: '#c9a900'
  on-tertiary-container: '#4c3f00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdad6'
  primary-fixed-dim: '#ffb4ab'
  on-primary-fixed: '#410002'
  on-primary-fixed-variant: '#93000d'
  secondary-fixed: '#e5e2e1'
  secondary-fixed-dim: '#c8c6c5'
  on-secondary-fixed: '#1c1b1b'
  on-secondary-fixed-variant: '#474746'
  tertiary-fixed: '#ffe16d'
  tertiary-fixed-dim: '#e9c400'
  on-tertiary-fixed: '#221b00'
  on-tertiary-fixed-variant: '#544600'
  background: '#121414'
  on-background: '#e2e2e2'
  surface-variant: '#333535'
typography:
  headline-lg:
    fontFamily: Anybody
    fontSize: 32px
    fontWeight: '800'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Anybody
    fontSize: 24px
    fontWeight: '800'
    lineHeight: '1.1'
  headline-md:
    fontFamily: Anybody
    fontSize: 20px
    fontWeight: '700'
    lineHeight: '1.2'
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.5'
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1.2'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style

This design system is engineered for the high-octane world of competitive motorsports. It targets athletes, team managers, and enthusiasts who require immediate access to data and communications in fast-paced environments. 

The aesthetic is **High-Contrast / Bold**, drawing inspiration from automotive instrument clusters and racing liveries. The UI prioritizes speed of recognition and visceral energy through a deep "Midnight Carbon" base punctuated by aggressive "Racing Red" accents. It avoids subtle decoration in favor of raw, functional impact, ensuring maximum legibility under varying lighting conditions common in outdoor sports environments.

## Colors

The palette is dominated by a pure dark theme to reduce eye strain and provide a premium, technical feel. 

- **Primary (Racing Red):** Reserved strictly for primary call-to-actions, critical status indicators, and active navigational states.
- **Secondary (Carbon Gray):** Used for surface elevation and secondary button backgrounds to create depth without sacrificing the dark aesthetic.
- **Tertiary (Warning Gold):** Utilized sparingly for alerts, achievements, or ranking highlights, providing a secondary point of visual interest.
- **Neutral:** Pure White (#FFFFFF) for primary text and high-contrast icons; Mid-grays (#8E8E93) for secondary metadata and disabled states.

## Typography

The typographic system utilizes **Anybody** for headlines—a variable font that evokes a sense of movement and mechanical precision. Its slightly expanded, heavy weights are used to anchor the page layout.

**Hanken Grotesk** serves as the workhorse for body and label text. It offers a clean, contemporary feel that remains legible in data-dense views like leaderboards or technical logs. 

- Use **Uppercase styling** for labels and navigation items to enhance the authoritative tone.
- Maintain tight line-heights on headlines to emphasize the "bold" personality of the brand.

## Layout & Spacing

The design system utilizes a **8px grid system** to ensure mathematical harmony across all components. 

- **Mobile:** 4-column fluid grid with 16px side margins and 16px gutters.
- **Desktop:** 12-column fixed grid (max-width 1200px) centered in the viewport.
- **Density:** The layout is high-density. Information is packed tightly in card-based modules to allow for quick scanning of data during active sessions. 
- **Vertical Rhythm:** Consistent use of 24px (lg) spacing between major sections and 8px (sm) between related elements within a card.

## Elevation & Depth

Depth is achieved through **Tonal Layering** rather than traditional shadows. In a dark environment, heavy shadows disappear; therefore, we use varying shades of gray to denote hierarchy.

- **Level 0 (Base):** #0F0F0F (Background)
- **Level 1 (Cards/Containers):** #1E1E1E (Subtle lift)
- **Level 2 (Active/Floating):** #2C2C2C (Overlays and tooltips)

To separate elements further, a **Low-contrast outline** (1px solid #333333) is applied to cards and input fields. This mimics the machined edges of automotive parts and ensures components remain distinct on the deep black canvas.

## Shapes

The shape language balances modern software trends with industrial design. A **Rounded (8px)** base radius is applied to all primary containers and buttons. This softening prevents the UI from appearing too aggressive or "hostile" while maintaining a professional, engineered look.

- **Chips/Filters:** Use the `rounded-xl` (24px) or pill-shape to distinguish them from actionable buttons.
- **Cards:** Strict 16px (rounded-lg) corner radius.
- **Icons:** Use "Sharp" or "Minimal" icons with a 2px stroke weight to match the technical nature of the typography.

## Components

### Buttons
- **Primary:** Solid Racing Red (#E31E24) with White text. Bold weight.
- **Secondary:** Solid Carbon Gray (#2C2C2C) with White text.
- **Ghost:** Transparent background with Racing Red outline and text.

### Cards
- Background: #1E1E1E.
- Border: 1px Solid #333333.
- Padding: 16px (md) or 20px for larger displays.
- Content should be grouped logically with clear labels in Hanken Grotesk.

### Input Fields
- Dark background (#1A1A1A) with a subtle 1px border. 
- Focused state: Border changes to Racing Red.
- Labels are always positioned above the field in `label-sm` uppercase.

### Navigation
- **Top Bar:** Fixed, #0F0F0F background, blurred backdrop.
- **Bottom Navigation (Mobile):** High-contrast White icons on a dark background. Active state is indicated by a Racing Red icon and text label.

### Chips/Filters
- Default: Dark gray background, light gray text.
- Active: Solid Racing Red background, white text.
- Shape: Fully rounded/pill to provide visual contrast against rectangular cards.
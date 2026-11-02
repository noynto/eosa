// Jetons du design system : voir docs/design/foundations.md (rôle Material de chaque couleur, contrastes mesurés).
tailwind.config = {
    theme: {
        extend: {
            colors: {
                bg: '#FDFBF7',
                'bg-soft': '#F4EFE5',
                'bg-border': '#F0E6D6',
                primary: '#601414',
                'primary-dark': '#3D0A0A',
                secondary: '#775546',
                accent: '#FFEFAE',
                // Texte discret : 5,1:1 sur bg, 4,6:1 sur bg-soft (WCAG AA).
                muted: '#7B6A4E',
                // Contour des champs et contrôles : 3,3:1 sur bg, 3,0:1 sur bg-soft.
                outline: '#9D8764',
                // 4,6:1 sur bg.
                success: '#188260',
                // 6,3:1 sur bg.
                error: '#B3261E',
            },
            fontFamily: {
                sans: ['DM Sans', 'system-ui', 'sans-serif'],
                serif: ['Cormorant Garamond', 'Georgia', 'serif'],
                display: ['Bagel Fat One', 'cursive'],
            },
        },
    },
    plugins: [
        // Règles de base communes à toutes les pages.
        function ({addBase, theme}) {
            addBase({
                // Focus clavier toujours visible, sur tout élément interactif.
                ':focus-visible': {
                    outline: `2px solid ${theme('colors.secondary')}`,
                    outlineOffset: '2px',
                },
                // Préférence système de réduction des mouvements.
                '@media (prefers-reduced-motion: reduce)': {
                    '*, ::before, ::after': {
                        animationDuration: '0.01ms !important',
                        animationIterationCount: '1 !important',
                        transitionDuration: '0.01ms !important',
                        scrollBehavior: 'auto !important',
                    },
                },
            });
        },
    ],
};

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#F5F0E8',
        ink: '#0D0D0D',
        accent: '#C8922A',
        'accent-light': '#E8B84B',
        muted: '#8A8070',
        border: '#1A1A1A',
        'surface-1': '#EDE8DC',
        'surface-2': '#E0DAC8',
        danger: '#C0392B',
        success: '#1A6B3A',
      },
      fontFamily: {
        display: ['"Playfair Display"', 'Georgia', 'serif'],
        mono: ['"DM Mono"', 'Courier New', 'monospace'],
        body: ['"DM Sans"', 'sans-serif'],
      },
      borderWidth: {
        '3': '3px',
      },
      boxShadow: {
        'brutal': '4px 4px 0px #0D0D0D',
        'brutal-lg': '6px 6px 0px #0D0D0D',
        'brutal-sm': '2px 2px 0px #0D0D0D',
        'brutal-accent': '4px 4px 0px #C8922A',
        'brutal-inset': 'inset 3px 3px 0px #0D0D0D',
      },
      animation: {
        'stamp': 'stamp 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards',
        'fadeUp': 'fadeUp 0.5s ease forwards',
        'scanline': 'scanline 3s linear infinite',
        'pulse-border': 'pulseBorder 2s ease-in-out infinite',
      },
      keyframes: {
        stamp: {
          '0%': { transform: 'scale(2) rotate(-12deg)', opacity: '0' },
          '60%': { transform: 'scale(0.95) rotate(1deg)', opacity: '1' },
          '100%': { transform: 'scale(1) rotate(0deg)', opacity: '1' },
        },
        fadeUp: {
          '0%': { transform: 'translateY(16px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
        scanline: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100vh)' },
        },
        pulseBorder: {
          '0%, 100%': { borderColor: '#C8922A' },
          '50%': { borderColor: '#E8B84B' },
        }
      }
    },
  },
  plugins: [],
}

/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'system-ui', 'sans-serif'],
            },
            colors: {
                background: '#F8FAFC', // Slate 50
                paper: '#FFFFFF',
                ink: {
                    900: '#0F172A', // Slate 900
                    700: '#334155', // Slate 700
                    500: '#64748B', // Slate 500
                    400: '#94A3B8', // Slate 400
                    200: '#E2E8F0', // Slate 200
                },
                accent: {
                    DEFAULT: '#6366f1', // Indigo 500
                    hover: '#4f46e5',   // Indigo 600
                    light: '#e0e7ff',   // Indigo 50
                },
                primary: {
                    50: '#f0f9ff',
                    100: '#e0f2fe',
                    500: '#0ea5e9', // Sky 500
                    600: '#0284c7', // Sky 600
                }
            },
            boxShadow: {
                'paper': '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
                'float': '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
                'glow': '0 0 15px rgba(99, 102, 241, 0.5)',
            },
            animation: {
                'fade-in-up': 'fadeInUp 0.5s ease-out forwards',
                'fade-in': 'fadeIn 0.5s ease-out forwards',
                'float': 'float 6s ease-in-out infinite',
                'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
            },
            keyframes: {
                fadeInUp: {
                    '0%': { opacity: '0', transform: 'translateY(20px)' },
                    '100%': { opacity: '1', transform: 'translateY(0)' },
                },
                fadeIn: {
                    '0%': { opacity: '0' },
                    '100%': { opacity: '1' },
                },
                float: {
                    '0%, 100%': { transform: 'translateY(0)' },
                    '50%': { transform: 'translateY(-10px)' },
                }
            }
        },
    },
    plugins: [],
}

/** @type {import('tailwindcss').Config} */
module.exports = {
  // src 폴더 안의 '모든' 폴더(**)와 파일을 바라보도록 설정해야 합니다.
  content: [
    "./src/**/*.{js,jsx,ts,tsx}", 
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
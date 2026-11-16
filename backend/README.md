cd D:\Rohit\SpendSenseAI2
git pull
Add-Content .gitignore "*.jks`n*.keystore`napp/release/"
git add .
git status
git commit -m "Update README, add license and screenshots"
git push
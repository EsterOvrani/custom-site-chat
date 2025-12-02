# 📸 Screenshots Guide

## How to Add Screenshots to Your README

### Option 1: Using GitHub (Recommended)

1. **Create screenshots folder** in your repository:
   ```
   frontend/
   └── screenshots/
       ├── hero-banner.png
       ├── login-page.png
       ├── dashboard.png
       └── ...
   ```

2. **Take screenshots** of your app:
   - Use your browser's screenshot tool (F12 → Device Toolbar)
   - Use tools like:
     - **Windows:** Snipping Tool / Snip & Sketch
     - **Mac:** Cmd + Shift + 4
     - **Linux:** Screenshot tool
     - **Browser Extension:** Awesome Screenshot

3. **Save screenshots** with descriptive names:
   ```
   ✅ Good: login-page.png, dashboard-with-documents.png
   ❌ Bad: Screenshot1.png, IMG_001.png
   ```

4. **Commit and push** to GitHub:
   ```bash
   git add screenshots/
   git commit -m "Add UI screenshots"
   git push
   ```

5. **Reference in README**:
   ```markdown
   ![Login Page](screenshots/login-page.png)
   ```

---

### Option 2: Drag & Drop in GitHub Editor

1. Go to your repository on GitHub
2. Click **"Edit"** on README.md
3. **Drag and drop** image directly into the editor
4. GitHub will auto-upload and create markdown link
5. Click **"Commit changes"**

---

### Option 3: Use Image Hosting (External)

If you don't want images in your repo:

**Imgur:**
```markdown
![Login Page](https://i.imgur.com/ABC123.png)
```

**GitHub Issues Trick:**
1. Create a new issue (don't submit)
2. Drag image into comment box
3. Copy generated URL
4. Use in README

---

## 📐 Recommended Screenshot Sizes

| Type | Resolution | Format |
|------|-----------|--------|
| **Hero Banner** | 1200x400 | PNG |
| **Full Page** | 1920x1080 | PNG |
| **Component** | 800x600 | PNG |
| **Mobile** | 375x667 | PNG |
| **GIF Demo** | 600x400 | GIF (< 5MB) |

---

## 🎨 Tools for Beautiful Screenshots

### **Browser Tools**
- [Screely](https://www.screely.com/) - Add browser mockup
- [Carbon](https://carbon.now.sh/) - Code screenshots
- [CleanShot X](https://cleanshot.com/) - Mac screenshot tool

### **Screen Recording (for GIFs)**
- [ScreenToGif](https://www.screentogif.com/) - Windows
- [Kap](https://getkap.co/) - Mac
- [Peek](https://github.com/phw/peek) - Linux

### **Image Optimization**
- [TinyPNG](https://tinypng.com/) - Compress PNG
- [Squoosh](https://squoosh.app/) - Image compression

---

## ✅ Screenshots Checklist

For each major page, take:

### **Authentication**
- [ ] Login page (empty state)
- [ ] Register page (empty state)
- [ ] Email verification page
- [ ] Google OAuth button
- [ ] Success message
- [ ] Error message

### **Dashboard**
- [ ] Empty state (no documents)
- [ ] With documents (2-3 cards)
- [ ] Upload modal open
- [ ] Processing status (15%, 50%, 80%)
- [ ] Completed document
- [ ] Mobile view

### **Settings**
- [ ] Collection settings page
- [ ] Secret key shown
- [ ] Embed code preview
- [ ] Copy confirmation toast
- [ ] Regenerate warning dialog

### **Chat Widget**
- [ ] Widget closed (button only)
- [ ] Widget open (empty)
- [ ] Widget with conversation
- [ ] Loading state (typing...)
- [ ] Limit reached warning
- [ ] RTL example (Hebrew)
- [ ] LTR example (English)

### **Responsive**
- [ ] Mobile view (< 768px)
- [ ] Tablet view (768-1024px)
- [ ] Desktop view (> 1024px)

---

## 🎬 Creating GIF Demos

1. **Record screen** using tool above
2. **Trim** to 5-10 seconds
3. **Optimize** (reduce size to < 5 MB)
4. **Upload** to repository or Imgur
5. **Use in README**:
   ```markdown
   ![Upload Demo](screenshots/upload-demo.gif)
   ```

**Pro Tips:**
- Keep GIFs short (5-10 seconds)
- Show one action per GIF
- Add cursor highlighting
- Use 10-15 FPS (smaller file size)

---

## 📝 Screenshot Naming Convention

```
Format: [page/feature]-[state]-[variant].png

Examples:
✅ login-page-empty.png
✅ dashboard-with-documents.png
✅ upload-progress-50percent.png
✅ chat-widget-open-mobile.png
✅ settings-page-desktop.png

❌ screen1.png
❌ IMG_001.png
❌ Screenshot 2024-12-02.png
```

---

## 🚀 Quick Start

1. **Install screenshot tool**
2. **Open your app** in browser
3. **Take screenshots** of each page
4. **Save** to `screenshots/` folder
5. **Optimize** images (compress)
6. **Commit** to GitHub
7. **Update** README with image paths

Done! 🎉

---

## 📚 Additional Resources

- [GitHub Markdown Guide](https://guides.github.com/features/mastering-markdown/)
- [How to add images to README](https://stackoverflow.com/questions/10189356/how-to-add-screenshot-to-readmes-in-github-repository)
- [Awesome README Examples](https://github.com/matiassingers/awesome-readme)

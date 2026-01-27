# راهنمای استایل پیام‌های خطا در فرم جنریتور

## مشکل:
المنت‌های `<small class="validation">` در فرم جنریتور سایز بزرگ دارند و باید کوچک‌تر شوند.

## راه‌حل:

### 1. افزودن استایل به فایل CSS/SCSS اصلی پروژه:

#### اگر از CSS استفاده می‌کنید:
فایل `form-generator-validation.css` را در پروژه Angular خود کپی کنید و به `angular.json` اضافه کنید:

```json
"styles": [
  "src/styles.css",
  "src/app/form-generator-validation.css"
]
```

#### اگر از SCSS استفاده می‌کنید:
فایل `form-generator-validation.scss` را import کنید در فایل اصلی `styles.scss`:

```scss
@import './form-generator-validation';
```

### 2. افزودن مستقیم به فایل CSS/SCSS موجود:

اگر فایل CSS/SCSS کامپوننت فرم جنریتور را دارید، این استایل را اضافه کنید:

```css
.validation {
  font-size: 0.7rem !important;
  color: #dc3545;
  display: block;
  margin-top: 0.25rem;
  line-height: 1.4;
}

small.validation {
  font-size: 0.7rem !important;
}
```

### 3. افزودن به فایل global styles:

اگر می‌خواهید این استایل به صورت global اعمال شود، به فایل `styles.css` یا `styles.scss` اضافه کنید:

```css
/* Global validation message styles */
.validation {
  font-size: 0.7rem !important;
  color: #dc3545;
  display: block;
  margin-top: 0.25rem;
  line-height: 1.4;
}
```

## سایزهای پیشنهادی:

- **0.7rem** - کوچک (پیشنهادی)
- **0.65rem** - خیلی کوچک
- **0.75rem** - کمی کوچک‌تر از پیش‌فرض

## مثال HTML:

```html
<small class="validation ng-star-inserted">
  شماره مالیاتی باید ۲۲ کاراکتر شامل حروف بزرگ و اعداد باشد.
</small>
```

## تست:

1. فرم را باز کنید
2. یک فیلد را با مقدار نامعتبر پر کنید
3. بررسی کنید که پیام خطا با سایز کوچک‌تر نمایش داده می‌شود

## نکات مهم:

- استفاده از `!important` برای اطمینان از اعمال استایل (در صورت وجود استایل‌های دیگر)
- رنگ `#dc3545` رنگ قرمز استاندارد Bootstrap برای خطا است
- `line-height: 1.4` برای خوانایی بهتر متن کوچک


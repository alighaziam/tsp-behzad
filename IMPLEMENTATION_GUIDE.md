# راهنمای پیاده‌سازی منطق انتخاب الگو

## تغییرات مورد نیاز

### 1. فایل TypeScript (advanced-invoice.component.ts)

#### تغییرات اصلی:

1. **افزودن Interface ها:**
   ```typescript
   interface TemplateField {
     id: string;
     code: string;
     title: string;
     requirementType: 'REQUIRED' | 'OPTIONAL' | 'CONDITIONALLY_REQUIRED' | 'IGNORE';
     enable: boolean;
     regex?: string;
     errorMessage?: string;
   }
   ```

2. **تغییر متد `onPatternSelect`:**
   - حذف منطق قدیمی
   - فراخوانی `GET /api/template-fields/template/code/{patternCode}`
   - استفاده از `buildFormFromFields` برای ساخت فرم

3. **افزودن متد `buildFormFromFields`:**
   - فیلتر کردن فیلدهای فعال
   - مرتب‌سازی فیلدها
   - ساخت FormControls با Validators مناسب

4. **افزودن متد `buildValidators`:**
   - بررسی `requirementType` برای `Validators.required`
   - استفاده از `regex` برای `Validators.pattern`

5. **افزودن متد `resetForm`:**
   - حذف تمام FormControls
   - Reset کردن فرم

6. **افزودن متدهای helper:**
   - `getErrorMessage`: دریافت پیام خطا از API
   - `hasError`: بررسی وجود خطا
   - `isRequired`: بررسی required بودن فیلد

### 2. فایل HTML (advanced-invoice.component.html)

#### تغییرات اصلی:

1. **استفاده از `*ngFor` برای فیلدهای داینامیک:**
   ```html
   <div *ngFor="let field of sortedHeaderFields" class="form-group">
   ```

2. **استفاده از `field.title` برای Label:**
   ```html
   <label>{{ field.title }}</label>
   ```

3. **استفاده از `field.code` برای formControlName:**
   ```html
   <input [formControlName]="field.code">
   ```

4. **نمایش پیام خطا:**
   ```html
   <div *ngIf="hasError(field.code)" class="error-message">
     {{ getErrorMessage(field.code) }}
   </div>
   ```

5. **نمایش علامت required:**
   ```html
   <span *ngIf="isRequired(field.code)" class="required-asterisk">*</span>
   ```

### 3. نکات مهم

#### مدیریت چرخه حیات:
- قبل از بارگذاری فیلدهای جدید، فرم باید reset شود
- استفاده از `takeUntil` برای جلوگیری از memory leak
- استفاده از `destroy$` Subject در `ngOnDestroy`

#### اعتبارسنجی:
- `Validators.required` برای فیلدهای `REQUIRED` و `CONDITIONALLY_REQUIRED`
- `Validators.pattern` با regex از API
- استفاده از `errorMessage` از API برای نمایش خطا

#### مدیریت خطا:
- بررسی وجود response و fields
- مدیریت خطاهای HTTP
- نمایش پیام مناسب به کاربر

## مثال استفاده:

```typescript
// در HTML:
<select (change)="onPatternSelect($event.target.value)">
  <option value="SALE_TEMPLATE_1_TYPE1">الگوی اول</option>
</select>

// با انتخاب الگو:
// 1. فرم reset می‌شود
// 2. API فراخوانی می‌شود
// 3. فیلدها از response خوانده می‌شوند
// 4. FormControls با Validators ساخته می‌شوند
// 5. فیلدها در HTML نمایش داده می‌شوند
```

## تست:

1. انتخاب الگو و بررسی بارگذاری فیلدها
2. تست اعتبارسنجی required
3. تست اعتبارسنجی pattern
4. تست نمایش errorMessage
5. تست reset فرم با تغییر الگو



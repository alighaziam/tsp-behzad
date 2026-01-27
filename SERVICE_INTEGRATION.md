# راهنمای یکپارچه‌سازی سرویس Template Fields

## Endpoint مورد استفاده

```
GET /api/template-fields/template/code/{templateCode}
```

## ساختار Response

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "fields": [
      {
        "id": "4684d3f3-215b-1938-e063-020011ac44ab",
        "code": "UNIQUE_TAX_ID",
        "title": "شماره منحصر به فرد مالیاتی",
        "requirementType": "REQUIRED",
        "enable": true,
        "regex": "^[A-Z0-9]{22}$",
        "errorMessage": "شماره مالیاتی باید ۲۲ کاراکتر شامل حروف بزرگ و اعداد باشد."
      },
      {
        "id": "4684d3f3-215c-1938-e063-020011ac44ab",
        "code": "INV_ISSUE_DT",
        "title": "تاریخ و زمان صدور صورتحساب (میلادی)",
        "requirementType": "REQUIRED",
        "enable": true,
        "regex": "^1\\d{12}$",
        "errorMessage": "تاریخ صدور باید ۱۳ رقم عددی باشد."
      }
    ],
    "templateTypes": [...]
  }
}
```

## Mapping Pattern Code به Template Code

برای select list که pattern number و invoice type دارد:

```typescript
// در onPatternSelect، باید pattern number و invoice type را به template code تبدیل کنید
// مثال:
// patternNumber = 1, invoiceType = 1 → "SALE_TEMPLATE_1_TYPE1"
// patternNumber = 1, invoiceType = 2 → "SALE_TEMPLATE_1_TYPE2"

private buildTemplateCode(patternNumber: number, invoiceType: number): string {
  const typeSuffix = invoiceType === 1 ? 'TYPE1' : 'TYPE2';
  const patternMap: { [key: number]: string } = {
    1: 'SALE_TEMPLATE_1',
    2: 'CURRENCY_SALE_TEMPLATE',
    3: 'GOLD_JEWELRY_TEMPLATE',
    4: 'CONTRACT_TEMPLATE',
    5: 'SERVICE_BILL_TEMPLATE',
    6: 'AIRLINE_TICKET_TEMPLATE',
    7: 'EXPORT_TEMPLATE',
    8: 'WAYBILL_TEMPLATE',
    9: 'PETROLEUM_PRODUCT_TEMPLATE',
    11: 'COMMODITY_EXCHANGE_TEMPLATE',
    13: 'INSURANCE_SERVICE_TEMPLATE'
  };
  
  const prefix = patternMap[patternNumber];
  return prefix ? `${prefix}_${typeSuffix}` : null;
}
```

## مثال کامل onPatternSelect

```typescript
onPatternSelect(patternValue: string): void {
  // patternValue می‌تواند pattern number باشد (مثل "1") یا template code کامل
  
  let templateCode: string;
  
  // اگر patternValue یک عدد است، باید به template code تبدیل شود
  if (/^\d+$/.test(patternValue)) {
    const patternNumber = parseInt(patternValue, 10);
    const invoiceType = this.getSelectedInvoiceType(); // از select دیگر
    templateCode = this.buildTemplateCode(patternNumber, invoiceType);
  } else {
    templateCode = patternValue; // مستقیماً template code است
  }
  
  if (!templateCode) {
    console.warn('Invalid template code');
    return;
  }
  
  this.loadTemplateFields(templateCode);
}

private loadTemplateFields(templateCode: string): void {
  this.resetForm();
  
  const url = `${this.apiBaseUrl}/template-fields/template/code/${templateCode}`;
  
  this.http.get<TemplateFieldsResponse>(url)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (response) => {
        if (response?.data?.fields?.length > 0) {
          this.buildFormFromFields(response.data.fields);
        } else {
          console.warn('No fields found');
        }
      },
      error: (error) => {
        console.error('Error loading fields:', error);
        // نمایش پیام خطا به کاربر
      }
    });
}
```

## نکات مهم

1. **مدیریت Response Wrapper:**
   - اگر response در `data` قرار دارد، از `response.data.fields` استفاده کنید
   - اگر مستقیماً `fields` است، از `response.fields` استفاده کنید

2. **Error Handling:**
   - بررسی وجود response
   - بررسی وجود fields
   - مدیریت خطاهای HTTP (404, 500, etc.)

3. **Performance:**
   - استفاده از `takeUntil` برای جلوگیری از memory leak
   - Cache کردن response اگر نیاز باشد

4. **Validation:**
   - بررسی regex قبل از استفاده در Validators.pattern
   - استفاده از try-catch برای ساخت RegExp



# دستورات cURL برای تست API های TaxpayerTaxProduct

## اطلاعات پایه
- **Base URL**: `http://localhost:8080` (یا آدرس سرور شما)
- **Context Path**: `/trusted-taxpayers-rs-1.0` (اگر GlassFish است) یا بدون context path
- **Base Endpoint**: `/api/taxpayer-tax-products`

## متغیرها
- `{baseUrl}`: `http://localhost:8080` یا آدرس سرور شما
- `{contextPath}`: `/trusted-taxpayers-rs-1.0` (در صورت نیاز)
- `{taxpayerId}`: شناسه UUID مودی مالیاتی (مثال: `550e8400-e29b-41d4-a716-446655440000`)
- `{taxProductId}`: شناسه UUID محصول مالیاتی (مثال: `660e8400-e29b-41d4-a716-446655440001`)

---

## 1. دریافت محصولات مالیاتی یک مودی مالیاتی (GET)

### cURL Command:
```bash
curl -X GET \
  '{baseUrl}{contextPath}/api/taxpayer-tax-products/taxpayer/{taxpayerId}/tax-products' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \
  -H 'Accept: application/json'
```

### مثال کامل:
```bash
curl -X GET \
  'http://localhost:8080/trusted-taxpayers-rs-1.0/api/taxpayer-tax-products/taxpayer/550e8400-e29b-41d4-a716-446655440000/tax-products' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json'
```

### برای Postman:
- **Method**: `GET`
- **URL**: `{{baseUrl}}/api/taxpayer-tax-products/taxpayer/{{taxpayerId}}/tax-products`
- **Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`
  - `Authorization: Bearer {{token}}` (در صورت نیاز)

---

## 2. ایجاد رابطه بین مودی مالیاتی و محصول مالیاتی (POST)

### cURL Command:
```bash
curl -X POST \
  '{baseUrl}{contextPath}/api/taxpayer-tax-products' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \
  -H 'Accept: application/json' \
  -d '{
    "taxpayerId": "{taxpayerId}",
    "taxProductId": "{taxProductId}"
  }'
```

### مثال کامل:
```bash
curl -X POST \
  'http://localhost:8080/trusted-taxpayers-rs-1.0/api/taxpayer-tax-products' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
    "taxpayerId": "550e8400-e29b-41d4-a716-446655440000",
    "taxProductId": "660e8400-e29b-41d4-a716-446655440001"
  }'
```

### برای Postman:
- **Method**: `POST`
- **URL**: `{{baseUrl}}/api/taxpayer-tax-products`
- **Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`
  - `Authorization: Bearer {{token}}` (در صورت نیاز)
- **Body** (raw JSON):
```json
{
  "taxpayerId": "550e8400-e29b-41d4-a716-446655440000",
  "taxProductId": "660e8400-e29b-41d4-a716-446655440001"
}
```

---

## 3. حذف رابطه بین مودی مالیاتی و محصول مالیاتی (DELETE)

### cURL Command:
```bash
curl -X DELETE \
  '{baseUrl}{contextPath}/api/taxpayer-tax-products/taxpayer/{taxpayerId}/tax-product/{taxProductId}' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \
  -H 'Accept: application/json'
```

### مثال کامل:
```bash
curl -X DELETE \
  'http://localhost:8080/trusted-taxpayers-rs-1.0/api/taxpayer-tax-products/taxpayer/550e8400-e29b-41d4-a716-446655440000/tax-product/660e8400-e29b-41d4-a716-446655440001' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json'
```

### برای Postman:
- **Method**: `DELETE`
- **URL**: `{{baseUrl}}/api/taxpayer-tax-products/taxpayer/{{taxpayerId}}/tax-product/{{taxProductId}}`
- **Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`
  - `Authorization: Bearer {{token}}` (در صورت نیاز)

---

## 4. دریافت تمام روابط (GET)

### cURL Command:
```bash
curl -X GET \
  '{baseUrl}{contextPath}/api/taxpayer-tax-products' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer YOUR_TOKEN_HERE' \
  -H 'Accept: application/json'
```

### مثال کامل:
```bash
curl -X GET \
  'http://localhost:8080/trusted-taxpayers-rs-1.0/api/taxpayer-tax-products' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json'
```

### برای Postman:
- **Method**: `GET`
- **URL**: `{{baseUrl}}/api/taxpayer-tax-products`
- **Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`
  - `Authorization: Bearer {{token}}` (در صورت نیاز)

---

## نکات مهم:

1. **UUID Format**: UUID ها باید در فرمت استاندارد باشند (با خط تیره)
   - صحیح: `550e8400-e29b-41d4-a716-446655440000`
   - غلط: `550e8400e29b41d4a716446655440000`

2. **Context Path**: اگر از GlassFish استفاده می‌کنید، context path احتمالاً `/trusted-taxpayers-rs-1.0` است.
   در Spring Boot standalone معمولاً context path وجود ندارد.

3. **Authentication**: اگر API شما نیاز به Authentication دارد، باید token را در header `Authorization` قرار دهید.

4. **Response Codes**:
   - `200 OK`: موفقیت‌آمیز
   - `201 Created`: ایجاد موفق (برای POST)
   - `204 No Content`: حذف موفق (برای DELETE)
   - `400 Bad Request`: اطلاعات ورودی نامعتبر
   - `404 Not Found`: رکورد یافت نشد
   - `409 Conflict`: رابطه تکراری است (برای POST)

---

## نمونه JSON Response (GET Tax Products):

```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "taxProductId": "TP-2024-001",
    "title": "محصول مالیاتی نمونه",
    "vat": 9,
    "taxable": "TAXABLE",
    "runDate": "2024-01-01",
    "expiration": "2024-12-31",
    "type": "EXCLUSIVE_DOMESTIC_PRODUCTION",
    "pricingDescription": "توضیحات قیمت‌گذاری",
    "taxCreateDate": "2024-01-01",
    "taxLastEditDate": "2024-01-15",
    "isActive": true,
    "createdOn": "2024-01-01T10:00:00Z",
    "updatedOn": "2024-01-15T10:00:00Z"
  }
]
```

## نمونه JSON Response (POST):

```json
{
  "taxpayerId": "550e8400-e29b-41d4-a716-446655440000",
  "taxProductId": "660e8400-e29b-41d4-a716-446655440001"
}
```







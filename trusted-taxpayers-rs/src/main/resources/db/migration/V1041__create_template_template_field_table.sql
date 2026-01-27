CREATE TABLE TEMPLATE_TEMPLATE_FIELD
(
    TEMPLATE_ID            RAW(16) NOT NULL,
    TEMPLATE_FIELD_ID      RAW(16) NOT NULL,
    FIELD_REQUIREMENT_TYPE VARCHAR2(30 CHAR) NOT NULL, -- REQUIRED, OPTIONAL, etc.
    FIELD_SECTION          VARCHAR2(20 CHAR) NOT NULL, -- HEADER, BODY, FOOTER, etc.
    ENABLE                 NUMBER(1) DEFAULT 1 NOT NULL,

    CONSTRAINT PK_TEMPLATE_TEMPLATE_FIELD
        PRIMARY KEY (TEMPLATE_ID, TEMPLATE_FIELD_ID),

    CONSTRAINT FK_TTF_TEMPLATE
        FOREIGN KEY (TEMPLATE_ID)
            REFERENCES TEMPLATE (ID)
            ON DELETE CASCADE,

    CONSTRAINT FK_TTF_FIELD
        FOREIGN KEY (TEMPLATE_FIELD_ID)
            REFERENCES TEMPLATE_FIELD (ID)
            ON DELETE CASCADE
);

CREATE INDEX IDX_TTF_TEMPLATE_ID ON TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID);
CREATE INDEX IDX_TTF_FIELD_ID ON TEMPLATE_TEMPLATE_FIELD (TEMPLATE_FIELD_ID);

--------------------------(Data Entry)---------------------
/*******************************************************************************
** TEMPLATE: Pattern 1 (General Sale) - SALE_TEMPLATE_1_TYPE1
** TOTAL RECORDS: 62
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (اجباری) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'creationDateTime', 'internalSerialNo', 'invoiceSubject',
                                'sellerTin', 'personType', 'buyerNationalId', 'totalDiscount', 'totalVat',
                                'totalOtherTax', 'totalBillAmount', 'totalNetWeight', 'cashAmount', 'productDescription',
                                'measurementUnit', 'currencyAmount', 'discountAmount', 'postDiscountAmount', 'vatRate',
                                'vatAmount', 'otherTaxSubject', 'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (در شرایط خاص اجباری) ========== */
           WHEN tf.TSP_CODE IN (
                                'invoiceType', 'invoicePattern', 'sellerBranchCode', 'creditAmount',
                                'totalVatShare', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount', 'constructionFee'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (اختیاری) ================= */
           ELSE 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount', 'cashShare', 'vatShare')
               THEN 'PAYMENT'
           /* بخش بدنه (BODY) - فقط فیلدهای مربوط به خودِ کالا */
           WHEN tf.INVOICE_STEP = 'ITEMS'
               AND tf.TSP_CODE NOT IN ('totalDiscount', 'totalVat', 'totalOtherTax', 'totalBillAmount', 'totalNetWeight', 'totalAfterDiscount', 'totalPreDiscount')
               THEN 'BODY'
           /* بخش عمومی (HEADER) - شامل فیلدهای عمومی و اطلاعات خریدار */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '1-SALE_TEMPLATE_1_TYPE1'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'flightType', 'customsCode', 'customsDeclarationNo', 'subscriptionBillId', 'totalDiscount',
                      'totalAfterDiscount', 'totalVat', 'totalOtherTax', 'totalBillAmount', 'totalNetWeight',
                      'cashAmount', 'creditAmount', 'totalVatShare', 'tax 17', 'productId',
                      'productDescription', 'quantity', 'measurementUnit', 'netWeight', 'unitPrice',
                      'currencyAmount', 'currencyType', 'exchangeRate', 'irrValue', 'discountAmount',
                      'postDiscountAmount', 'vatRate', 'vatAmount', 'otherTaxSubject', 'otherTaxRate',
                      'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'constructionFee',
                      'cashShare', 'vatShare', 'commissionContractNo', 'totalProductAmount', 'paymentSwitchNo',
                      'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo', 'payerCardNo',
                      'payerNationalId', 'paymentDate', 'paidAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 2 (Currency Sale) - CURRENCY_SALE_TEMPLATE_TYPE1
** TOTAL RECORDS: 59
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (اجباری) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                                'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId', 'currencyAmount',
                                'currencyType', 'exchangeRate', 'preDiscountAmount', 'discountAmount',
                                'postDiscountAmount', 'vatRate', 'vatAmount', 'totalProductAmount',
                                'currencyBuyRate', 'currencyVatBase'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (در شرایط خاص اجباری) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'buyerNationalId', 'buyerTaxId',
                                'buyerPassportNo', 'cashAmount', 'creditAmount', 'otherTaxSubject',
                                'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate',
                                'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (اختیاری) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                                'customsPermitNo', 'customsCode', 'contractNumber', 'totalDiscount',
                                'totalVatShare', 'taxArticle17', 'productDescription', 'cashShare',
                                'vatShare', 'commissionContractNo', 'paymentSwitchNo', 'merchantId',
                                'terminalId', 'paymentMethod', 'trackReferenceNo', 'payerCardNo',
                                'payerNationalId', 'paymentDate', 'paidAmount'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                                'currencyBuyRate', 'currencyVatBase', 'vatBaseAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount',
                                'settlementMethod')
               THEN 'PAYMENT'
           /* بخش عمومی (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '2-CURRENCY_SALE_TEMPLATE_TYPE1'
    /* فیلتر کردن موارد Ignore شده در لیست شما */
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject', 'sellerTin',
                      'personType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'settlementMethod', 'productId', 'currencyAmount', 'currencyType',
                      'exchangeRate', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate',
                      'vatAmount', 'totalProductAmount', 'currencyBuyRate', 'currencyVatBase', 'creationDateTime',
                      'referenceTaxId', 'buyerNationalId', 'buyerTaxId', 'buyerPassportNo', 'cashAmount',
                      'creditAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                      'otherLegalRate', 'otherLegalAmount', 'internalSerialNo', 'sellerBranchCode',
                      'buyerPostalCode', 'buyerBranchCode', 'customsPermitNo', 'customsCode', 'contractNumber',
                      'totalDiscount', 'totalVatShare', 'taxArticle17', 'productDescription', 'cashShare',
                      'vatShare', 'commissionContractNo', 'paymentSwitchNo', 'merchantId', 'terminalId',
                      'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 3 (Gold, Jewelry and Platinum) - GOLD_JEWELRY_TEMPLATE_TYPE1
** TOTAL RECORDS: 58
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (اجباری) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                                'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId', 'quantity',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'constructionFee', 'sellerProfit', 'commissionFee',
                                'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (در شرایط خاص اجباری) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'buyerTaxId', 'cashAmount', 'creditAmount',
                                'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (اختیاری) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'buyerNationalId', 'sellerBranchCode', 'buyerPostalCode',
                                'buyerBranchCode', 'contractNumber', 'totalDiscount', 'totalVatShare',
                                'taxArticle17', 'productDescription', 'measurementUnit', 'currencyAmount',
                                'currencyType', 'exchangeRate', 'cashShare', 'vatShare', 'commissionContractNo',
                                'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo',
                                'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount', 'purity'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) - شامل محاسبات اختصاصی طلا */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'constructionFee', 'sellerProfit',
                                'commissionFee', 'purity', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'vatBaseAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount',
                                'settlementMethod')
               THEN 'PAYMENT'
           /* بخش عمومی (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '3-GOLD_JEWELRY_TEMPLATE_TYPE1'
    /* فیلتر کردن موارد Ignore شده در لیست طلا */
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject', 'sellerTin',
                      'personType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'settlementMethod', 'productId', 'quantity', 'unitPrice',
                      'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount',
                      'constructionFee', 'sellerProfit', 'commissionFee', 'totalProductAmount', 'creationDateTime',
                      'referenceTaxId', 'buyerTaxId', 'cashAmount', 'creditAmount', 'otherTaxSubject',
                      'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                      'internalSerialNo', 'buyerNationalId', 'sellerBranchCode', 'buyerPostalCode',
                      'buyerBranchCode', 'contractNumber', 'totalDiscount', 'totalVatShare', 'taxArticle17',
                      'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType', 'exchangeRate',
                      'cashShare', 'vatShare', 'commissionContractNo', 'paymentSwitchNo', 'merchantId',
                      'terminalId', 'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                      'paymentDate', 'paidAmount', 'purity'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 4 (Contracting) - CONTRACT_TEMPLATE_TYPE1
** TOTAL RECORDS: 55
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (اجباری) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'contractNumber', 'totalPreDiscount', 'totalAfterDiscount',
                                'totalVat', 'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId',
                                'quantity', 'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (در شرایط خاص اجباری) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'buyerTaxId', 'cashAmount', 'creditAmount',
                                'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (اختیاری) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'buyerNationalId', 'sellerBranchCode', 'buyerPostalCode',
                                'buyerBranchCode', 'totalDiscount', 'totalVatShare', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'cashShare', 'vatShare', 'commissionContractNo',
                                'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                                'currencyAmount', 'currencyType', 'exchangeRate', 'vatBaseAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount',
                                'settlementMethod')
               THEN 'PAYMENT'
           /* بخش عمومی (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '4-CONTRACT_TEMPLATE_TYPE1'
    /* حذف فیلدهای Ignore شده */
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject', 'sellerTin',
                      'personType', 'contractNumber', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                      'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId', 'quantity',
                      'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate',
                      'vatAmount', 'totalProductAmount', 'creationDateTime', 'referenceTaxId', 'buyerTaxId',
                      'cashAmount', 'creditAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                      'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'internalSerialNo',
                      'buyerNationalId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalDiscount', 'totalVatShare', 'taxArticle17', 'productDescription', 'measurementUnit',
                      'currencyAmount', 'currencyType', 'exchangeRate', 'cashShare', 'vatShare',
                      'commissionContractNo', 'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                      'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 5 (Service Bills) - SERVICE_BILL_TEMPLATE_TYPE1
** TOTAL RECORDS: 48
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (اجباری) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'subscriptionBillId', 'totalPreDiscount',
                                'totalAfterDiscount', 'totalVat', 'totalOtherTax', 'totalBillAmount',
                                'productId', 'quantity', 'unitPrice', 'preDiscountAmount', 'discountAmount',
                                'postDiscountAmount', 'vatRate', 'vatAmount', 'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (در شرایط خاص اجباری) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'otherTaxSubject', 'otherTaxRate',
                                'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (اختیاری) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'buyerNationalId', 'buyerTaxId', 'sellerBranchCode',
                                'buyerPostalCode', 'buyerBranchCode', 'totalDiscount', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'commissionContractNo',
                                'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'vatBaseAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'subscriptionBillId')
               THEN 'PAYMENT'
           /* بخش عمومی (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '5-SERVICE_BILL_TEMPLATE_TYPE1'
    /* فیلتر فیلدهای Ignore شده بر اساس لیست شما */
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject', 'sellerTin',
                      'personType', 'subscriptionBillId', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                      'totalOtherTax', 'totalBillAmount', 'productId', 'quantity', 'unitPrice',
                      'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount',
                      'totalProductAmount', 'creationDateTime', 'referenceTaxId', 'otherTaxSubject',
                      'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate',
                      'otherLegalAmount', 'internalSerialNo', 'buyerNationalId', 'buyerTaxId', 'sellerBranchCode',
                      'buyerPostalCode', 'buyerBranchCode', 'totalDiscount', 'taxArticle17', 'productDescription',
                      'measurementUnit', 'commissionContractNo', 'paymentSwitchNo', 'merchantId', 'terminalId',
                      'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);

/*******************************************************************************
** TEMPLATE: Pattern 6 (Airline Ticket) - AIRLINE_TICKET_TEMPLATE_TYPE1
** TOTAL RECORDS: 58
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (اجباری) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'flightType', 'totalPreDiscount', 'totalAfterDiscount',
                                'totalVat', 'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId',
                                'quantity', 'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (در شرایط خاص اجباری) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'buyerNationalId', 'buyerPassportNo',
                                'cashAmount', 'creditAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (اختیاری) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode',
                                'buyerBranchCode', 'totalDiscount', 'totalVatShare', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'cashShare', 'vatShare', 'commissionContractNo',
                                'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate',
                                'paidAmount', 'agencyTin'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                                'currencyAmount', 'currencyType', 'exchangeRate', 'vatBaseAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount',
                                'settlementMethod')
               THEN 'PAYMENT'
           /* بخش عمومی (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '6-AIRLINE_TICKET_TEMPLATE_TYPE1'
    /* حذف موارد Ignore شده طبق لیست */
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject', 'sellerTin',
                      'personType', 'flightType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                      'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId', 'quantity',
                      'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate',
                      'vatAmount', 'totalProductAmount', 'creationDateTime', 'referenceTaxId', 'buyerNationalId',
                      'buyerPassportNo', 'cashAmount', 'creditAmount', 'otherTaxSubject', 'otherTaxRate',
                      'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'internalSerialNo',
                      'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode', 'totalDiscount',
                      'totalVatShare', 'taxArticle17', 'productDescription', 'measurementUnit', 'currencyAmount',
                      'currencyType', 'exchangeRate', 'cashShare', 'vatShare', 'commissionContractNo',
                      'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo',
                      'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount', 'agencyTin'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 7 (Export) - EXPORT_TEMPLATE_TYPE1
** TOTAL RECORDS: 48
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (اجباری) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'totalVat', 'totalOtherTax', 'totalBillAmount', 'totalIrrValue',
                                'totalCurrencyValue', 'settlementMethod', 'productId', 'quantity', 'currencyType',
                                'exchangeRate', 'irrValue', 'currencyValue', 'vatRate', 'vatAmount',
                                'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (در شرایط خاص اجباری) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'totalNetWeight', 'cashAmount',
                                'creditAmount', 'netWeight', 'otherTaxSubject', 'otherTaxRate',
                                'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (اختیاری) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'personType', 'buyerNationalId', 'buyerTaxId', 'sellerBranchCode',
                                'buyerPostalCode', 'buyerBranchCode', 'buyerPassportNo', 'customsCode',
                                'customsDeclarationNo', 'customsDeclarationDate', 'totalVatShare',
                                'productDescription', 'measurementUnit', 'unitPrice', 'cashShare', 'vatShare',
                                'commissionContractNo'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'netWeight', 'unitPrice', 'currencyType', 'exchangeRate',
                                'irrValue', 'currencyValue', 'vatRate', 'vatAmount',
                                'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'vatBaseAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('settlementMethod', 'cashAmount', 'creditAmount', 'cashShare', 'vatShare')
               THEN 'PAYMENT'
           /* بخش عمومی و گمرکی (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '7-EXPORT_TEMPLATE_TYPE1'
    /* اعمال فیلتر بر اساس موارد غیر Ignore در لیست صادرات */
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject', 'sellerTin',
                      'totalVat', 'totalOtherTax', 'totalBillAmount', 'totalIrrValue', 'totalCurrencyValue',
                      'settlementMethod', 'productId', 'quantity', 'currencyType', 'exchangeRate', 'irrValue',
                      'currencyValue', 'vatRate', 'vatAmount', 'totalProductAmount', 'creationDateTime',
                      'referenceTaxId', 'totalNetWeight', 'cashAmount', 'creditAmount', 'netWeight',
                      'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate',
                      'otherLegalAmount', 'internalSerialNo', 'personType', 'buyerNationalId', 'buyerTaxId',
                      'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode', 'buyerPassportNo', 'customsCode',
                      'customsDeclarationNo', 'customsDeclarationDate', 'totalVatShare', 'productDescription',
                      'measurementUnit', 'unitPrice', 'cashShare', 'vatShare', 'commissionContractNo'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);

/*******************************************************************************
** TEMPLATE: Pattern 8 (Waybill) - WAYBILL_TEMPLATE_TYPE1
** TOTAL RECORDS: 71
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (31 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                                'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId', 'quantity',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'totalProductAmount', 'waybillNo', 'originCountry',
                                'destinationCountry', 'senderNationalId', 'waybillType', 'fleetNumber',
                                'transportGoods', 'transportProductId', 'transportProductDesc' -- اصلاح شد
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (15 Fields) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'buyerTaxId', 'cashAmount', 'creditAmount',
                                'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount', 'originCity', 'destinationCity',
                                'receiverNationalId', 'driverNationalId'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (25 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'buyerNationalId', 'sellerBranchCode', 'buyerPostalCode',
                                'buyerBranchCode', 'totalDiscount', 'totalVatShare', 'taxArticle17',
                                'productDescription', 'currencyAmount', 'currencyType', 'exchangeRate',
                                'cashShare', 'vatShare', 'commissionContractNo', 'paymentSwitchNo',
                                'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo',
                                'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount', 'referenceWaybillNo'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'unitPrice',
                                'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'totalProductAmount',
                                'transportGoods', 'transportProductId', 'transportProductDesc',
                                'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                                'currencyAmount', 'currencyType', 'exchangeRate')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount',
                                'settlementMethod', 'cashShare', 'vatShare')
               THEN 'PAYMENT'
           /* سایر موارد (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '8-WAYBILL_TEMPLATE_TYPE1'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalPreDiscount', 'totalDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'settlementMethod', 'cashAmount', 'creditAmount', 'totalVatShare',
                      'taxArticle17', 'productId', 'productDescription', 'quantity', 'unitPrice',
                      'currencyAmount', 'currencyType', 'exchangeRate', 'preDiscountAmount', 'discountAmount',
                      'postDiscountAmount', 'vatRate', 'vatAmount', 'otherTaxSubject', 'otherTaxRate',
                      'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                      'cashShare', 'vatShare', 'commissionContractNo', 'totalProductAmount', 'paymentSwitchNo',
                      'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo', 'payerCardNo',
                      'payerNationalId', 'paymentDate', 'paidAmount', 'waybillNo', 'referenceWaybillNo',
                      'originCountry', 'originCity', 'destinationCountry', 'destinationCity',
                      'senderNationalId', 'receiverNationalId', 'waybillType', 'fleetNumber',
                      'driverNationalId', 'transportGoods', 'transportProductId', 'transportProductDesc'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 9 (Petroleum Products) - PETROLEUM_PRODUCT_TEMPLATE_TYPE1
** TOTAL RECORDS: 58
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (21 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                                'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId', 'quantity',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatAmount', 'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (13 Fields) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'buyerTaxId', 'cashAmount', 'creditAmount',
                                'vatRate', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'vatBaseAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (25 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'buyerNationalId', 'sellerBranchCode', 'buyerPostalCode',
                                'buyerBranchCode', 'totalDiscount', 'totalVatShare', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'cashShare', 'vatShare', 'commissionContractNo',
                                'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) - شامل فیلد کلیدی vatBaseAmount */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'vatBaseAmount', 'otherTaxSubject',
                                'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount', 'currencyAmount',
                                'currencyType', 'exchangeRate')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount',
                                'settlementMethod', 'cashShare', 'vatShare')
               THEN 'PAYMENT'
           /* سایر موارد (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '9-PETROLEUM_PRODUCT_TEMPLATE_TYPE1'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalPreDiscount', 'totalDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'settlementMethod', 'cashAmount', 'creditAmount', 'totalVatShare',
                      'taxArticle17', 'productId', 'productDescription', 'quantity', 'measurementUnit',
                      'unitPrice', 'currencyAmount', 'currencyType', 'exchangeRate', 'preDiscountAmount',
                      'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount', 'otherTaxSubject',
                      'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate',
                      'otherLegalAmount', 'cashShare', 'vatShare', 'commissionContractNo',
                      'totalProductAmount', 'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                      'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount',
                      'vatBaseAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 11 (Commodity Exchange) - COMMODITY_EXCHANGE_TEMPLATE_TYPE1
** TOTAL RECORDS: 24
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (20 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'productId', 'quantity', 'measurementUnit', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'totalProductAmount', 'totalAfterDiscount', 'totalVat',
                                'totalBillAmount', 'stockExchangeAnnouncementNo', 'stockExchangeAnnouncementDate',
                                'preDiscountAmount', 'totalPreDiscount' -- اضافه شده برای تکمیل لیست اجباری ها بر اساس ساختار استاندارد
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (2 Fields) =========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* =================== OPTIONAL (4 Fields) =================== */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'sellerBranchCode', 'productDescription', 'purity'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'postDiscountAmount', 'vatRate', 'vatAmount', 'purity',
                                'stockExchangeAnnouncementNo', 'stockExchangeAnnouncementDate',
                                'preDiscountAmount', 'totalProductAmount')
               THEN 'BODY'
           /* سایر موارد (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '11-COMMODITY_EXCHANGE_TEMPLATE_TYPE1'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'sellerBranchCode',
                      'totalAfterDiscount', 'totalVat', 'totalBillAmount', 'productId', 'productDescription',
                      'quantity', 'measurementUnit', 'postDiscountAmount', 'vatRate', 'vatAmount',
                      'totalProductAmount', 'purity', 'stockExchangeAnnouncementNo', 'stockExchangeAnnouncementDate',
                      'preDiscountAmount', 'totalPreDiscount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Pattern 13 (Insurance Services) - INSURANCE_SERVICE_TEMPLATE_TYPE1
** TOTAL RECORDS: 58
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (21 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'personType', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat',
                                'totalOtherTax', 'totalBillAmount', 'settlementMethod', 'productId', 'quantity',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (11 Fields) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'buyerTaxId', 'cashAmount', 'creditAmount',
                                'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (26 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'buyerNationalId', 'sellerBranchCode', 'buyerPostalCode',
                                'buyerBranchCode', 'totalDiscount', 'totalVatShare', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'cashShare', 'vatShare', 'commissionContractNo',
                                'totalProductAmount', 'paymentSwitchNo', 'merchantId', 'terminalId',
                                'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount', 'totalProductAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'cashAmount', 'creditAmount',
                                'settlementMethod', 'cashShare', 'vatShare')
               THEN 'PAYMENT'
           /* سایر موارد (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '13-INSURANCE_SERVICE_TEMPLATE_TYPE1'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalPreDiscount', 'totalDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'settlementMethod', 'cashAmount', 'creditAmount', 'totalVatShare',
                      'taxArticle17', 'productId', 'productDescription', 'quantity', 'measurementUnit',
                      'unitPrice', 'currencyAmount', 'currencyType', 'exchangeRate', 'preDiscountAmount',
                      'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount', 'otherTaxSubject',
                      'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate',
                      'otherLegalAmount', 'cashShare', 'vatShare', 'commissionContractNo', 'totalProductAmount',
                      'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo',
                      'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID
                    AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Type 2 - Pattern 1 (Sale) - SALE_TEMPLATE_1_TYPE2
** TOTAL RECORDS: 55
** CORRECTION: CURR_VAL moved from OPTIONAL to REQUIRED
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (
    TEMPLATE_ID,
    TEMPLATE_FIELD_ID,
    FIELD_REQUIREMENT_TYPE,
    FIELD_SECTION,
    ENABLE
)
SELECT
    t.ID,
    tf.ID,
    CASE
        /* ================= REQUIRED (19 Fields) ================= */
        WHEN tf.TSP_CODE IN (
                             'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                             'sellerTin', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                             'totalBillAmount', 'taxArticle17', 'productDescription', 'netWeight',
                             'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate', 'currencyValue'
            ) THEN 'REQUIRED'

        /* ========== CONDITIONALLY REQUIRED (9 Fields) ========== */
        WHEN tf.TSP_CODE IN (
                             'creationDateTime', 'referenceTaxId', 'vatAmount', 'otherTaxSubject',
                             'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate',
                             'otherLegalAmount'
            ) THEN 'CONDITIONALLY_REQUIRED'

        /* ================= OPTIONAL (27 Fields) ================= */
        WHEN tf.TSP_CODE IN (
                             'internalSerialNo', 'personType', 'buyerNationalId', 'buyerTaxId', 'sellerBranchCode',
                             'buyerPostalCode', 'buyerBranchCode', 'totalDiscount', 'totalVatShare', 'productId',
                             'quantity', 'unitPrice', 'currencyAmount', 'currencyType', 'cashShare', 'vatShare',
                             'commissionContractNo', 'totalProductAmount', 'paymentSwitchNo', 'merchantId',
                             'terminalId', 'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                             'paymentDate', 'paidAmount'
            ) THEN 'OPTIONAL'

        ELSE 'IGNORE'
        END AS FIELD_REQUIREMENT_TYPE,

    CASE
        /* بخش بدنه (BODY) */
        WHEN tf.TSP_CODE IN (
                             'productId', 'productDescription', 'quantity', 'netWeight', 'unitPrice',
                             'currencyAmount', 'currencyType', 'currencyValue', 'preDiscountAmount',
                             'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount',
                             'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                             'otherLegalRate', 'otherLegalAmount', 'totalProductAmount'
            ) THEN 'BODY'
        /* بخش پرداخت (PAYMENT) */
        WHEN tf.TSP_CODE IN (
                             'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                             'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate',
                             'paidAmount', 'cashShare', 'vatShare'
            ) THEN 'PAYMENT'
        /* سایر موارد (HEADER) */
        ELSE 'HEADER'
        END AS FIELD_SECTION,
    1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '1-SALE_TEMPLATE_1_TYPE2'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalPreDiscount', 'totalDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'totalVatShare', 'taxArticle17', 'productId', 'productDescription',
                      'quantity', 'netWeight', 'unitPrice', 'currencyAmount', 'currencyType', 'currencyValue',
                      'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount',
                      'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                      'otherLegalRate', 'otherLegalAmount', 'cashShare', 'vatShare', 'commissionContractNo',
                      'totalProductAmount', 'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                      'trackReferenceNo', 'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount'
    )
  AND NOT EXISTS (
    SELECT 1 FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
    WHERE x.TEMPLATE_ID = t.ID AND x.TEMPLATE_FIELD_ID = tf.ID
);
/*******************************************************************************
** TEMPLATE: Type 2 - Pattern 3 (Gold) - GOLD_JEWELRY_TEMPLATE_TYPE2
** TOTAL RECORDS: 57 (EXACT COUNT)
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (24 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                                'totalBillAmount', 'productId', 'quantity', 'unitPrice', 'preDiscountAmount',
                                'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount', 'constructionFee',
                                'sellerProfit', 'commissionFee', 'totalFeeProfit', 'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (11 Fields) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId',
                                'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount',
                                'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                                'vatAmount' -- در این الگو طبق لیست شما در شرایط خاص اجباری است
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (25 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'personType', 'buyerNationalId', 'buyerTaxId', 'sellerBranchCode',
                                'buyerPostalCode', 'buyerBranchCode', 'totalDiscount', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'commissionContractNo', 'paymentSwitchNo', 'merchantId', 'terminalId',
                                'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'purity'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'constructionFee', 'sellerProfit',
                                'commissionFee', 'totalFeeProfit', 'purity', 'currencyAmount',
                                'currencyType', 'exchangeRate', 'otherTaxSubject', 'otherTaxRate',
                                'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount',
                                'totalProductAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount')
               THEN 'PAYMENT'
           /* سایر موارد (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '3-GOLD_JEWELRY_TEMPLATE_TYPE2'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalPreDiscount', 'totalDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'taxArticle17', 'productId', 'productDescription', 'quantity',
                      'measurementUnit', 'unitPrice', 'currencyAmount', 'currencyType', 'exchangeRate',
                      'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount',
                      'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                      'otherLegalRate', 'otherLegalAmount', 'constructionFee', 'sellerProfit',
                      'commissionFee', 'totalFeeProfit', 'commissionContractNo', 'totalProductAmount',
                      'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo',
                      'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount', 'purity'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Type 2 - Pattern 9 (Petroleum) - PETROLEUM_PRODUCT_TEMPLATE_TYPE2
** TOTAL RECORDS: 53
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (19 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                                'totalBillAmount', 'productId', 'quantity', 'unitPrice', 'preDiscountAmount',
                                'discountAmount', 'postDiscountAmount', 'vatAmount', 'totalProductAmount'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (10 Fields) ========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'vatRate', 'otherTaxSubject',
                                'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate',
                                'otherLegalAmount', 'vatBaseAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (26 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'personType', 'buyerNationalId', 'buyerTaxId', 'sellerBranchCode',
                                'buyerPostalCode', 'buyerBranchCode', 'totalDiscount', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'commissionContractNo', 'paymentSwitchNo', 'merchantId', 'terminalId',
                                'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'vatBaseAmount', 'otherTaxSubject',
                                'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount', 'currencyAmount',
                                'currencyType', 'exchangeRate', 'totalProductAmount')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount')
               THEN 'PAYMENT'
           /* سایر موارد (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '9-PETROLEUM_PRODUCT_TEMPLATE_TYPE2'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalPreDiscount', 'totalDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'taxArticle17', 'productId', 'productDescription', 'quantity',
                      'measurementUnit', 'unitPrice', 'currencyAmount', 'currencyType', 'exchangeRate',
                      'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount',
                      'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                      'otherLegalRate', 'otherLegalAmount', 'commissionContractNo', 'totalProductAmount',
                      'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo',
                      'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount', 'vatBaseAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID AND x.TEMPLATE_FIELD_ID = tf.ID);
/*******************************************************************************
** TEMPLATE: Type 2 - Pattern 13 (Insurance) - INSURANCE_SERVICE_TEMPLATE_TYPE2
** TOTAL RECORDS: 55
*******************************************************************************/
INSERT INTO HAM_TSP.TEMPLATE_TEMPLATE_FIELD (TEMPLATE_ID,
                                             TEMPLATE_FIELD_ID,
                                             FIELD_REQUIREMENT_TYPE,
                                             FIELD_SECTION,
                                             ENABLE)
SELECT t.ID,
       tf.ID,
       CASE
           /* ================= REQUIRED (21 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'taxId', 'invoiceDateTime', 'invoiceType', 'invoicePattern', 'invoiceSubject',
                                'sellerTin', 'totalPreDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                                'totalBillAmount', 'productId', 'quantity', 'unitPrice', 'preDiscountAmount',
                                'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount', 'totalProductAmount',
                                'insuranceUniqueId'
               ) THEN 'REQUIRED'

           /* ========== CONDITIONALLY REQUIRED (8 Fields) =========== */
           WHEN tf.TSP_CODE IN (
                                'creationDateTime', 'referenceTaxId', 'otherTaxSubject', 'otherTaxRate',
                                'otherTaxAmount', 'otherLegalSubject', 'otherLegalRate', 'otherLegalAmount'
               ) THEN 'CONDITIONALLY_REQUIRED'

           /* ================= OPTIONAL (27 Fields) ================= */
           WHEN tf.TSP_CODE IN (
                                'internalSerialNo', 'personType', 'buyerNationalId', 'buyerTaxId', 'sellerBranchCode',
                                'buyerPostalCode', 'buyerBranchCode', 'totalDiscount', 'taxArticle17',
                                'productDescription', 'measurementUnit', 'currencyAmount', 'currencyType',
                                'exchangeRate', 'commissionContractNo', 'paymentSwitchNo', 'merchantId', 'terminalId',
                                'paymentMethod', 'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount', 'endorsementUniqueId', 'vatBaseAmount'
               ) THEN 'OPTIONAL'
           END AS FIELD_REQUIREMENT_TYPE,

       CASE
           /* بخش بدنه (BODY) */
           WHEN tf.TSP_CODE IN ('productId', 'productDescription', 'quantity', 'measurementUnit',
                                'unitPrice', 'preDiscountAmount', 'discountAmount', 'postDiscountAmount',
                                'vatRate', 'vatAmount', 'totalProductAmount', 'insuranceUniqueId',
                                'endorsementUniqueId', 'vatBaseAmount', 'otherTaxSubject',
                                'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                                'otherLegalRate', 'otherLegalAmount', 'currencyAmount',
                                'currencyType', 'exchangeRate')
               THEN 'BODY'
           /* بخش پرداخت (PAYMENT) */
           WHEN tf.TSP_CODE IN ('paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod',
                                'trackReferenceNo', 'payerCardNo', 'payerNationalId',
                                'paymentDate', 'paidAmount')
               THEN 'PAYMENT'
           /* سایر موارد (HEADER) */
           ELSE 'HEADER'
           END AS FIELD_SECTION,
       1 AS ENABLE
FROM HAM_TSP.TEMPLATE t
         CROSS JOIN HAM_TSP.TEMPLATE_FIELD tf
WHERE t.CODE = '13-INSURANCE_SERVICE_TEMPLATE_TYPE2'
  AND tf.TSP_CODE IN (
                      'taxId', 'invoiceDateTime', 'creationDateTime', 'invoiceType', 'internalSerialNo',
                      'referenceTaxId', 'invoicePattern', 'invoiceSubject', 'sellerTin', 'personType',
                      'buyerNationalId', 'buyerTaxId', 'sellerBranchCode', 'buyerPostalCode', 'buyerBranchCode',
                      'totalPreDiscount', 'totalDiscount', 'totalAfterDiscount', 'totalVat', 'totalOtherTax',
                      'totalBillAmount', 'taxArticle17', 'productId', 'productDescription', 'quantity',
                      'measurementUnit', 'unitPrice', 'currencyAmount', 'currencyType', 'exchangeRate',
                      'preDiscountAmount', 'discountAmount', 'postDiscountAmount', 'vatRate', 'vatAmount',
                      'otherTaxSubject', 'otherTaxRate', 'otherTaxAmount', 'otherLegalSubject',
                      'otherLegalRate', 'otherLegalAmount', 'commissionContractNo', 'totalProductAmount',
                      'paymentSwitchNo', 'merchantId', 'terminalId', 'paymentMethod', 'trackReferenceNo',
                      'payerCardNo', 'payerNationalId', 'paymentDate', 'paidAmount', 'insuranceUniqueId',
                      'endorsementUniqueId', 'vatBaseAmount'
    )
  AND NOT EXISTS (SELECT 1
                  FROM HAM_TSP.TEMPLATE_TEMPLATE_FIELD x
                  WHERE x.TEMPLATE_ID = t.ID AND x.TEMPLATE_FIELD_ID = tf.ID);


COMMIT;
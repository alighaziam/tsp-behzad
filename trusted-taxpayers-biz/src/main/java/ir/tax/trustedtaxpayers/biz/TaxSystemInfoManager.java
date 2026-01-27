package ir.tax.trustedtaxpayers.biz;

import ir.tax.trustedtaxpayers.beans.dto.tax.*;

/**
 * مدیریت اطلاعات سامانه مالیاتی
 * Tax System Information Manager
 * 
 * مشابه Manager ها در samt-git
 */
public interface TaxSystemInfoManager extends GenericBiz {

    /**
     * دریافت توکن احراز هویت
     * Get authentication token
     * 
     * @return مدل توکن
     */
    TokenModel getToken();

    /**
     * درخواست توکن جدید
     * Request new token
     * 
     * @return مدل توکن جدید
     */
    TokenModel requestNewToken();

    /**
     * دریافت اطلاعات سرور
     * Get server information
     * 
     * @return اطلاعات سرور
     */
    ServerInformationModel getServerInformation();

    /**
     * دریافت اطلاعات حافظه مالیاتی
     * Get fiscal memory information
     * 
     * @param fiscalId شناسه حافظه مالیاتی
     * @return اطلاعات حافظه مالیاتی
     */
    FiscalInformationModel getFiscalInformation(String fiscalId);

    /**
     * دریافت اطلاعات کد اقتصادی
     * Get economic code information
     * 
     * @param economicCode کد اقتصادی
     * @return اطلاعات کد اقتصادی
     */
    EconomicCodeModel getEconomicCodeInformation(String economicCode);

    /**
     * جستجوی لیست سرویس‌ها و خدمات
     * Search service stuff list
     * 
     * @param searchDto معیارهای جستجو
     * @return نتایج جستجو
     */
    SearchResultModel<ServiceStuffModel> searchServiceStuff(SearchDto searchDto);
}



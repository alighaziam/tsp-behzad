package ir.gov.tax.tpis.sdk.content.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.gov.tax.tpis.sdk.content.config.PacketType;
import ir.gov.tax.tpis.sdk.transfer.api.TransferApi;
import ir.gov.tax.tpis.sdk.transfer.config.TransferConstants;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.dto.ErrorModel;
import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.gov.tax.tpis.sdk.transfer.dto.SyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;
import ir.gov.tax.tpis.sdk.transfer.impl.encrypter.DefaultEncrypter;
import ir.tax.trustedtaxpayers.beans.dto.tax.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DefaultTaxApiClient implements TaxApi {

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(DefaultTaxApiClient.class);
    private final TransferApi transferApi;
    private final String clientId;
    private TokenModel token;

    public DefaultTaxApiClient(TransferApi transferApi, String clientId) {
        this.transferApi = transferApi;
        this.clientId = clientId;
    }

    @Override
    public TokenModel requestToken() throws TaxApiException {
        GetTokenDto data = new GetTokenDto();
        data.setUsername(clientId);
        PacketDto packetDto = packetDtoBuilder(data, PacketType.PACKET_TYPE_GET_TOKEN);
        SyncResponseModel response = transferApi.sendPacket(packetDto, null, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to request token: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        Object responseData = response.getResult().getData();
        TokenModel tokenModel = mapper.convertValue(responseData, TokenModel.class);

        if (tokenModel != null) {
            this.token = tokenModel;
        }

        return tokenModel;
    }

    @Override
    public AsyncResponseModel sendInvoices(List<InvoiceDtoWrapper> invoices,
                                           Map<String, String> headers) throws TaxApiException {
        ArrayList<PacketDto> packets = new ArrayList<>();
        for (InvoiceDtoWrapper invoiceDto : invoices) {
            PacketDto e = packetDtoBuilder(invoiceDto.getInvoice(), "INVOICE.V01");
            if (invoiceDto.getFiscalId() != null && invoiceDto.getFiscalId().length() > 0) {
                e.setFiscalId(invoiceDto.getFiscalId());
            }
            if (invoiceDto.getUid() != null && invoiceDto.getUid().length() > 0) {
                e.setUid(invoiceDto.getUid());
            }
            packets.add(e);
        }

        if (headers == null) {
            headers = new HashMap<>();
        }
        if (token != null) {
            headers.computeIfAbsent(TransferConstants.AUTHORIZATION_HEADER, k -> "Bearer " + token.getToken());
        }
        return transferApi.sendPackets(packets, headers, true, true);
    }

    @Override
    public AsyncResponseModel sendMyInvoices(List<InvoiceDto> invoices,
                                             Map<String, String> headers) throws TaxApiException {
        ArrayList<PacketDto> packets = new ArrayList<>();
        for (InvoiceDto invoiceDto : invoices) {
            PacketDto e = packetDtoBuilder(invoiceDto, "INVOICE.V01");
            packets.add(e);
        }
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (token != null) {
            headers.computeIfAbsent(TransferConstants.AUTHORIZATION_HEADER, k -> "Bearer " + token.getToken());
        }
        return transferApi.sendPackets(packets, headers, true, true);
    }

    @Override
    public AsyncResponseModel sendInvoices(List<InvoiceDtoWrapper> invoices) throws TaxApiException {
        return sendInvoices(invoices, null);
    }

    @Override
    public AsyncResponseModel sendMyInvoices(List<InvoiceDto> invoices) throws TaxApiException {
        return sendMyInvoices(invoices, null);
    }

    @Override
    public ServerInformationModel getServerInformation() throws TaxApiException {
        PacketDto packetDto = packetDtoBuilder(null, PacketType.PACKET_TYPE_GET_SERVER_INFORMATION);
        SyncResponseModel response = transferApi.sendPacket(packetDto, null, false, false);

        if (response == null) {
            throw new TaxApiException("Failed to get server information: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        if (response.getResult() == null) {
            throw new TaxApiException("Response result is null");
        }

        Object responseData = response.getResult().getData();
        if (responseData == null) {
            throw new TaxApiException("Response data is null");
        }

        ServerInformationModel si = mapper.convertValue(responseData, ServerInformationModel.class);

        if (transferApi.getConfig().getEncrypter() == null && si.getPublicKeys() != null && !si.getPublicKeys().isEmpty()) {
            KeyDto key = si.getPublicKeys().get(0);
            DefaultEncrypter encrypter = new DefaultEncrypter(key.getKey(), key.getId());
            transferApi.getConfig().encrypter(encrypter);
        }

        return si;
    }


    @Override
    @SuppressWarnings("deprecation")
    public List<InquiryResultModel> inquiryByUidAndFiscalId(List<UidAndFiscalId> uidAndFiscalIds) throws TaxApiException {
        PacketDto packetDto = packetDtoBuilder(uidAndFiscalIds, PacketType.PACKET_TYPE_INQUIRY_BY_UID);
        HashMap<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token.getToken());
        }
        SyncResponseModel response = transferApi.sendPacket(packetDto, headers, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to inquiry by UID and Fiscal ID: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        Object responseData = response.getResult().getData();
        return fillInquiry(responseData);
    }

    @Override
    public List<InquiryResultModel> inquiryByTime(String persianTime) throws TaxApiException {
        InquiryByTimeDto data = new InquiryByTimeDto();
        data.setTime(persianTime);
        PacketDto packetDto = packetDtoBuilder(data, PacketType.PACKET_TYPE_INQUIRY_BY_TIME);
        HashMap<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token.getToken());
        }
        SyncResponseModel response = transferApi.sendPacket(packetDto, headers, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to inquiry by time: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        Object responseData = response.getResult().getData();
        return fillInquiry(responseData);
    }

    @Override
    public List<InquiryResultModel> inquiryByTimeRange(String startDatePersian,
                                                                                             String toDatePersian) throws TaxApiException {
        InquiryByTimeRangeDto data = new InquiryByTimeRangeDto();
        data.setStartDate(startDatePersian);
        data.setEndDate(toDatePersian);
        PacketDto packetDto = packetDtoBuilder(data, PacketType.PACKET_TYPE_INQUIRY_BY_TIME_RANGE);
        HashMap<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token.getToken());
        }
        SyncResponseModel response = transferApi.sendPacket(packetDto, headers, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to inquiry by time range: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        Object responseData = response.getResult().getData();
        return fillInquiry(responseData);
    }

    @Override
    public List<InquiryResultModel> inquiryByReferenceId(List<String> referenceIds) throws TaxApiException {
        InquiryByReferenceNumberDto data = new InquiryByReferenceNumberDto();
        data.setReferenceNumber(referenceIds);
        PacketDto packetDto = packetDtoBuilder(data, PacketType.PACKET_TYPE_INQUIRY_BY_REFERENCE_NUMBER);
        HashMap<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token.getToken());
        }
        SyncResponseModel response = transferApi.sendPacket(packetDto, headers, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to inquiry by reference ID: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        Object responseData = response.getResult().getData();
        return fillInquiry(responseData);
    }

    @Override
    public FiscalInformationModel getFiscalInformation(String fiscalId) throws TaxApiException {
        PacketDto packetDto = packetDtoBuilder(fiscalId, PacketType.PACKET_TYPE_GET_FISCAL_INFORMATION);
        packetDto.setFiscalId(fiscalId);
        HashMap<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token.getToken());
        }
        SyncResponseModel response = transferApi.sendPacket(packetDto, headers, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to get fiscal information: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            ErrorModel errorModel = response.getErrors().get(0);
            throw new TaxApiException(errorModel.getCode(),
                                      "ERROR " + errorModel.getCode() + ": " + errorModel.getMessage());
        }

        Object responseData = response.getResult().getData();
        String packetType = response.getResult().getPacketType();
        if (packetType.equals("ERROR")) {
            InvoiceErrorModel errorModel = mapper.convertValue(((List) responseData).get(0), InvoiceErrorModel.class);
            throw new TaxApiException(errorModel.getCode(),
                                      "ERROR " + errorModel.getCode() + ": " + errorModel.getMessage());
        }
        return mapper.convertValue(responseData, FiscalInformationModel.class);
    }

    @Override
    public SearchResultModel<ServiceStuffModel> getServiceStuffList(SearchDto searchDTO) throws TaxApiException {
        PacketDto packetDto = packetDtoBuilder(searchDTO, PacketType.PACKET_TYPE_GET_SERVICE_STUFF_LIST);
        HashMap<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token.getToken());
        }
        SyncResponseModel response = transferApi.sendPacket(packetDto, headers, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to get service stuff list: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        Object responseData = response.getResult().getData();
        return mapper.convertValue(responseData, new TypeReference<SearchResultModel<ServiceStuffModel>>() {
        });
    }

    @Override
    public EconomicCodeModel getEconomicCodeInformation(String economicCode) throws TaxApiException {
        EconomicCodeDto data = new EconomicCodeDto();
        data.setEconomicCode(economicCode);
        PacketDto packetDto = packetDtoBuilder(data, PacketType.PACKET_TYPE_GET_ECONOMIC_CODE_INFORMATION);
        HashMap<String, String> headers = new HashMap<>();
        if (token != null) {
            headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token.getToken());
        }
        SyncResponseModel response = transferApi.sendPacket(packetDto, headers, false, false);

        // Handle null response (e.g., network errors, empty response body)
        if (response == null) {
            throw new TaxApiException("Failed to get economic code information: No response received from server. This may be due to network connectivity issues or the server being unavailable.");
        }

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new TaxApiException(response.getErrors().get(0).getMessage());
        }

        Object responseData = response.getResult().getData();
        String packetType = response.getResult().getPacketType();
        if (packetType.equals("ERROR")) {
            InvoiceErrorModel errorModel = mapper.convertValue(((List) responseData).get(0), InvoiceErrorModel.class);
            throw new TaxApiException(errorModel.getCode(),
                                      "ERROR " + errorModel.getCode() + ": " + errorModel.getMessage());
        }
        return mapper.convertValue(responseData, EconomicCodeModel.class);
    }

    public TokenModel getToken() {
        return token;
    }

    public void setToken(TokenModel token) {
        this.token = token;
    }

    private List<InquiryResultModel> fillInquiry(Object responseData) {
        List<InquiryResultModel> inquiryResultModels =
                mapper.convertValue(responseData, new TypeReference<List<InquiryResultModel>>() {
                });
        for (InquiryResultModel inquiryResultModel : inquiryResultModels) {
            if (inquiryResultModel.getPacketType() == null || !inquiryResultModel.getPacketType()
                                                                                 .equalsIgnoreCase(
                                                                                         "RECEIVE_INVOICE_CONFIRM")) {
                continue;
            }
            CreateInvoiceResponse data = inquiryResultModel.getData();
            inquiryResultModel.setData(data);
        }
        return inquiryResultModels;
    }

    private PacketDto packetDtoBuilder(Object data, String packetType) {
        PacketDto packetDto = new PacketDto();
        packetDto.setUid(UUID.randomUUID().toString());
        packetDto.setPacketType(packetType);
        packetDto.setFiscalId(clientId);
        packetDto.setData(data);
        packetDto.setRetry(false);
        return packetDto;
    }
}

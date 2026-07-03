package com.lifecell.diia.be.service;


import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.model.dto.CustomerIdDocument;
import com.lifecell.diia.be.model.dto.CustomerIdDocumentType;
import com.lifecell.diia.be.util.DiiaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.gov.diia.document.GetDriverLicenseToProcessRequest;
import ua.gov.diia.document.GetPensionCardToProcessReq;
import ua.gov.diia.document.TaxpayerCardInDocumentV1;
import ua.gov.diia.documentsservice.DocumentsServiceGrpc;
import ua.gov.diia.documentsservice.GetPassportToProcessRequestV3;
import ua.gov.diia.notification.CreateNotificationWithPushesRequest;
import ua.gov.diia.notification.NotificationServiceGrpc;
import ua.gov.diia.user.UserServiceGrpc;
import ua.gov.diia.user.userdocuments.DocumentFilter;
import ua.gov.diia.user.userdocuments.HasDocumentsFilter;
import ua.gov.diia.user.userdocuments.HasDocumentsRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiiaService {
    private final DocumentsServiceGrpc.DocumentsServiceBlockingStub documentService;
    private final NotificationServiceGrpc.NotificationServiceBlockingStub notificationService;
    private final UserServiceGrpc.UserServiceBlockingStub userService;

    private final List<Integer> DOC_STATUS_ACTIVE;

    public DiiaService(
            Properties properties,
            DocumentsServiceGrpc.DocumentsServiceBlockingStub documentService,
            NotificationServiceGrpc.NotificationServiceBlockingStub notificationService,
            UserServiceGrpc.UserServiceBlockingStub userService) {
        this.documentService = documentService;
        this.notificationService = notificationService;
        this.userService = userService;

        DOC_STATUS_ACTIVE = Optional.ofNullable(properties.getService().getDiia().getDocument().getStates())
                .orElse(Collections.emptyList());
    }

    public boolean userHasTaxDocument(String transactionId, String userRef) {
        return !(userHasDocuments(transactionId, userRef, Set.of(CustomerIdDocumentType.ID_CODE)).isEmpty());
    }

    public boolean userHasSomeIdDocument(String transactionId, String userRef) {
        return !(userHasDocuments(transactionId, userRef, Set.of(CustomerIdDocumentType.PASSPORT_INT, CustomerIdDocumentType.PASSPORT_EXT, CustomerIdDocumentType.DRIVER_LICENCE)).isEmpty());
    }

    public boolean userHasPcDocument(String transactionId, String userRef) {
        return !(userHasDocuments(transactionId, userRef, Set.of(CustomerIdDocumentType.PENSION_CARD)).isEmpty());
    }

    public CustomerIdDocument userGetTaxDocument(String transactionId, String userRef) {
        if (userHasTaxDocument(transactionId, userRef)) {
            var docs = userGetDocuments(transactionId, userRef, Set.of(CustomerIdDocumentType.ID_CODE));
            if (!(docs.isEmpty())) {
                return docs.iterator().next();
            }
        }
        return null;
    }

    public CustomerIdDocument userGetPcDocument(String transactionId, String userRef) {
        if (userHasPcDocument(transactionId, userRef)) {
            var docs = userGetDocuments(transactionId, userRef, Set.of(CustomerIdDocumentType.PENSION_CARD));
            if (!(docs.isEmpty())) {
                return docs.iterator().next();
            } else {
                log.warn("Consistency failed for user pension card: transactionId={}, userRef={}", transactionId, userRef);
            }
        }
        return null;
    }

    public CustomerIdDocument userGetLatestIdDocument(String transactionId, String userRef) {
        var has = userHasDocuments(transactionId, userRef, Set.of(CustomerIdDocumentType.PASSPORT_INT, CustomerIdDocumentType.PASSPORT_EXT, CustomerIdDocumentType.DRIVER_LICENCE));
        if (!(has.isEmpty())) {
            var docs = userGetDocuments(transactionId, userRef, has);
            {
                var doc = docs.stream()
                        .filter((d) -> d.getType() == CustomerIdDocumentType.PASSPORT_INT)
                        .findAny()
                        .orElse(null);
                if (doc != null) {
                    return doc;
                }
            }
            {
                var doc = docs.stream()
                        .filter((d) -> d.getType() == CustomerIdDocumentType.PASSPORT_EXT)
                        .findAny()
                        .orElse(null);
                if (doc != null) {
                    return doc;
                }
            }
            {
                var doc = docs.stream()
                        .filter((d) -> d.getType() == CustomerIdDocumentType.DRIVER_LICENCE)
                        .findAny()
                        .orElse(null);
                if (doc != null) {
                    return doc;
                }
            }
            log.warn("Consistency failed for user id document: transactionId={}, userRef={}", transactionId, userRef);
        }
        return null;
    }

    private Set<CustomerIdDocumentType> userHasDocuments(String transactionId, String userRef, Set<CustomerIdDocumentType> types) {
        var su = DiiaUtils.getSessionUser();

        var has = userService.hasDocuments(
                HasDocumentsRequest.newBuilder()
                        .setUserIdentifier(su.getIdentifier())
                        .addAllFilters(types.stream()
                                .map((type) ->
                                        HasDocumentsFilter.newBuilder()
                                                .addOneOf(DocumentFilter.newBuilder()
                                                        .setDocumentType(type.getRef())
                                                        .addAllDocStatus(DOC_STATUS_ACTIVE)
                                                        .build())
                                                .build())
                                .toList())
                        .build());

        var result = new HashSet<>(types);
        if (has.getMissingDocumentsCount() > 0) {
            for (var typeRef : has.getMissingDocumentsList()) {
                var type = CustomerIdDocumentType.fromRef(typeRef);
                if (type != null) {
                    result.remove(type);
                }
            }
        }

        log.info("User has documents: transactionId = {}, userRef = {}, request = {}, response = {}",
                transactionId, userRef,
                types.stream().map((t) -> t.getRef()).collect(Collectors.joining(",")),
                result.stream().map((t) -> t.getRef()).collect(Collectors.joining(",")));
        return result;
    }

    private Set<CustomerIdDocument> userGetDocuments(String transactionId, String userRef, Set<CustomerIdDocumentType> types) {
        var su = DiiaUtils.getSessionUser();

        var result = new HashSet<CustomerIdDocument>();

        /*
        var pds = documentService.getPassportsToProcess(
                GetPassportsToProcessRequest.newBuilder()
                        .setItn(session.getUser().getItn())
                        .setFirstName(session.getUser().getFName())
                        .setLastName(session.getUser().getLName())
                        .setMiddleName(session.getUser().getMName())
                        .build());
        */

        /*
        var t = new HashSet<>(types);
        t.remove(CustomerIdDocumentType.ID_CODE);
        t.remove(CustomerIdDocumentType.PENSION_CARD);
        t.add(CustomerIdDocumentType.PASSPORT_INT);
        t.add(CustomerIdDocumentType.PASSPORT_EXT);
        var pds = documentService.getDocumentsToProcess(
                GetDocumentsToProcessRequest.newBuilder()
                        .addAllDocumentTypes(types.stream().map((type) -> type.getRef()).toList())
                        .build());
         */

        var pds = safeCall(
                () -> {
                    var tmp = documentService.getPassportToProcessV3(
                            GetPassportToProcessRequestV3.newBuilder()
                                    .setIdentifier(su.getIdentifier())
                                    .setItn(su.getItn())
                                    .setFirstName(su.getFName())
                                    .setLastName(su.getLName())
                                    .setMiddleName(su.getMName())
                                    .setHandlePhoto(false)
                                    .build());

                    log.info("User has passport documents: transactionId = {}, userRef = {}, internal passport = {} - {} - {} - {}, foreign passport = {} - {} - {} - {}",
                            transactionId, userRef,
                            tmp.hasInternalPassport(),
                            (tmp.hasInternalPassport() ? tmp.getInternalPassport().getDocStatus() : 0),
                            (tmp.hasInternalPassport() ? tmp.getInternalPassport().hasTaxpayerCard() : false),
                            ((tmp.hasInternalPassport() && tmp.getInternalPassport().hasTaxpayerCard()) ? tmp.getInternalPassport().getTaxpayerCard().getStatus() : 0),
                            tmp.hasForeignPassport(),
                            (tmp.hasForeignPassport() ? tmp.getForeignPassport().getDocStatus() : 0),
                            (tmp.hasForeignPassport() ? tmp.getForeignPassport().hasTaxpayerCard() : false),
                            ((tmp.hasForeignPassport() && tmp.getForeignPassport().hasTaxpayerCard()) ? tmp.getForeignPassport().getTaxpayerCard().getStatus() : 0));

                    return tmp;
                },
                (e) -> {
                    log.warn("Error getting passport documents: transactionId = {}, userRef = {}", transactionId, userRef, e);
                });

        var icd = Optional.ofNullable(pds)
                .filter((d) -> d.hasInternalPassport())
                .map((d) -> d.getInternalPassport())
                .filter((d) -> d.hasTaxpayerCard())
                .map((d) -> d.getTaxpayerCard())
                .filter((t) -> DOC_STATUS_ACTIVE.contains(t.getStatus()))
                .orElse(Optional.ofNullable(pds)
                        .filter((d) -> d.hasForeignPassport())
                        .map((d) -> d.getForeignPassport())
                        .filter((d) -> d.hasTaxpayerCard())
                        .map((d) -> d.getTaxpayerCard())
                        .filter((t) -> DOC_STATUS_ACTIVE.contains(t.getStatus()))
                        .orElse(TaxpayerCardInDocumentV1.newBuilder()
                                .setNumber(su.getItn())
                                .setStatus(DOC_STATUS_ACTIVE.isEmpty() ? 0 : DOC_STATUS_ACTIVE.get(0))
                                .build()));

        if (types.contains(CustomerIdDocumentType.PASSPORT_INT)) {
            if ((pds != null) && pds.hasInternalPassport()) {
                if (DOC_STATUS_ACTIVE.isEmpty() || DOC_STATUS_ACTIVE.contains(pds.getInternalPassport().getDocStatus())) {
                    var src = pds.getInternalPassport();
                    var dst = new CustomerIdDocument();
                    dst.setType(CustomerIdDocumentType.PASSPORT_INT);
                    dst.setFirstName(oneOf(src.getFirstNameUA(), src.getFirstNameEN()));
                    dst.setSecondName(oneOf(src.getLastNameUA(), src.getLastNameEN()));
                    dst.setMiddleName(src.getMiddleNameUA());
                    dst.setBirthday(src.getBirthday());
                    dst.setSeries(src.getSeries());
                    dst.setNumber(src.getNumber());
                    dst.setAuthority(src.getDepartment());
                    dst.setFromDate(src.getIssueDate());
                    dst.setToDate(src.getExpirationDate());
                    dst.setGender(oneOf(src.getGenderUA(), src.getGenderEN()));
                    dst.setEmail(null);
                    dst.setAddress(src.getCurrentRegistrationPlaceUA());
                    if (icd != null) {
                        dst.setCode(icd.getNumber());
                    }
                    result.add(dst);
                } else {
                    log.warn("User has internal passport document but: transactionId = {}, userRef = {}, status = {}",
                            transactionId, userRef,
                            pds.getInternalPassport().getDocStatus());
                }
            } else {
                log.warn("User has no internal passport document: transactionId = {}, userRef = {}",
                        transactionId, userRef);
            }
        }
        if (types.contains(CustomerIdDocumentType.PASSPORT_EXT)) {
            if ((pds != null) && pds.hasForeignPassport()) {
                if (DOC_STATUS_ACTIVE.isEmpty() || DOC_STATUS_ACTIVE.contains(pds.getForeignPassport().getDocStatus())) {
                    var src = pds.getForeignPassport();
                    var dst = new CustomerIdDocument();
                    dst.setType(CustomerIdDocumentType.PASSPORT_EXT);
                    dst.setFirstName(oneOf(src.getFirstNameUA(), src.getFirstNameEN()));
                    dst.setSecondName(oneOf(src.getLastNameUA(), src.getLastNameEN()));
                    dst.setMiddleName(src.getMiddleNameUA());
                    dst.setBirthday(src.getBirthday());
                    dst.setSeries(src.getSeries());
                    dst.setNumber(src.getNumber());
                    dst.setAuthority(oneOf(src.getDepartmentUA(), src.getDepartmentEN()));
                    dst.setFromDate(src.getIssueDate());
                    dst.setToDate(src.getExpirationDate());
                    dst.setGender(oneOf(src.getGenderUA(), src.getGenderEN()));
                    dst.setEmail(null);
                    dst.setAddress(src.getCurrentRegistrationPlaceUA());
                    if (icd != null) {
                        dst.setCode(icd.getNumber());
                    }
                    result.add(dst);
                } else {
                    log.warn("User has foreign passport document but: transactionId = {}, userRef = {}, status = {}",
                            transactionId, userRef,
                            pds.getForeignPassport().getDocStatus());
                }
            } else {
                log.warn("User has no foreign passport document: transactionId = {}, userRef = {}",
                        transactionId, userRef);
            }
        }

        if (types.contains(CustomerIdDocumentType.DRIVER_LICENCE)) {
            var dds = safeCall(
                    () -> {
                        var tmp = documentService.getDriverLicenseToProcessV3(
                                GetDriverLicenseToProcessRequest.newBuilder()
                                        .setItn(su.getItn())
                                        .setIgnoreCache(false)
                                        .build());

                        log.info("User has driver license document: transactionId = {}, userRef = {}, document = {} - {}",
                                transactionId, userRef,
                                tmp.hasDriverLicense(), (tmp.hasDriverLicense() ? tmp.getDriverLicense().getDocStatus() : -1));

                        return tmp;
                    },
                    (e) -> {
                        log.warn("Error getting driver license document: transactionId = {}, userRef = {}", transactionId, userRef, e);
                    });
            if ((dds != null) && dds.hasDriverLicense()) {
                if (DOC_STATUS_ACTIVE.isEmpty() || DOC_STATUS_ACTIVE.contains(dds.getDriverLicense().getDocStatus())) {
                    var src = dds.getDriverLicense();
                    var dst = new CustomerIdDocument();
                    dst.setType(CustomerIdDocumentType.DRIVER_LICENCE);
                    dst.setFirstName(oneOf(src.getFirstNameUA(), src.getFirstNameEN()));
                    dst.setSecondName(oneOf(src.getLastNameUA(), src.getLastNameEN()));
                    dst.setMiddleName(src.getMiddleNameUA());
                    dst.setBirthday(src.getBirthday());
                    dst.setSeries(src.getSerial());
                    dst.setNumber(src.getNumber());
                    dst.setAuthority(src.getDepartment());
                    dst.setFromDate(src.getIssueDate());
                    dst.setToDate(src.getExpirationDate());
                    dst.setGender(null);
                    dst.setEmail(null);
                    dst.setAddress(null);
                    if (icd != null) {
                        dst.setCode(icd.getNumber());
                    }
                    result.add(dst);
                } else {
                    log.warn("User has driver license document but: transactionId = {}, userRef = {}, status = {}",
                            transactionId, userRef,
                            pds.getForeignPassport().getDocStatus());
                }
            } else {
                log.warn("User has no driver license document: transactionId = {}, userRef = {}",
                        transactionId, userRef);
            }
        }

        if (types.contains(CustomerIdDocumentType.PENSION_CARD)) {
            var pc = safeCall(
                    () -> {
                        var tmp = documentService.getPensionCardToProcess(
                                GetPensionCardToProcessReq.newBuilder()
                                        .setIdentifier(su.getIdentifier())
                                        .setFName(su.getFName())
                                        .setLName(su.getLName())
                                        .setMName(su.getMName())
                                        .setItn(su.getItn())
                                        .setGender(su.getGender().name())
                                        .setBirthDay(su.getBirthDay())
                                        .build());

                        log.info("User has pension card document: transactionId = {}, userRef = {},  document = {} - {} - {}",
                                transactionId, userRef,
                                tmp.hasPensionCard(),
                                (tmp.hasPensionCard() ? tmp.getPensionCard().hasBaseData() : false),
                                ((tmp.hasPensionCard() && tmp.getPensionCard().hasBaseData()) ? tmp.getPensionCard().getBaseData().getStatus() : -1));

                        return tmp;
                    },
                    (e) -> {
                        log.warn("Error getting pension card document: transactionId = {}, userRef = {}", transactionId, userRef, e);
                    });
            if ((pc != null) && pc.hasPensionCard() && pc.getPensionCard().hasBaseData()) {
                if (DOC_STATUS_ACTIVE.isEmpty() || DOC_STATUS_ACTIVE.contains(pc.getPensionCard().getBaseData().getStatus())) {
                    var src = pc.getPensionCard();
                    var dst = new CustomerIdDocument();
                    dst.setType(CustomerIdDocumentType.PENSION_CARD);
                    dst.setFirstName(src.getFirstNameUA());
                    dst.setSecondName(src.getLastNameUA());
                    dst.setMiddleName(src.getMiddleNameUA());
                    dst.setBirthday(src.getBirthday());
                    dst.setSeries(null);
                    dst.setNumber(src.hasBaseData() ? src.getBaseData().getNumber() : null);
                    dst.setAuthority(null);
                    dst.setFromDate(src.hasBaseData() ? src.getBaseData().getIssuedAt() : null);
                    dst.setToDate(src.hasBaseData() ? src.getBaseData().getExpiresAt() : null);
                    dst.setGender(src.getGender());
                    dst.setEmail(null);
                    dst.setAddress(null);
                    if (icd != null) {
                        dst.setCode(icd.getNumber());
                    }
                    result.add(dst);
                } else {
                    log.warn("User has pension card document but: transactionId = {}, userRef = {}, status = {}",
                            transactionId, userRef,
                            pc.getPensionCard().getBaseData().getStatus());
                }
            } else {
                log.warn("User has no pension card document: transactionId = {}, userRef = {}",
                        transactionId, userRef);
            }
        }

        if (types.contains(CustomerIdDocumentType.ID_CODE)) {
            if (icd != null) {
                var src = icd;
                var dst = new CustomerIdDocument();
                dst.setType(CustomerIdDocumentType.ID_CODE);
                dst.setFirstName(null);
                dst.setSecondName(null);
                dst.setMiddleName(null);
                dst.setBirthday(null);
                dst.setSeries(null);
                dst.setNumber(src.getNumber());
                dst.setAuthority(null);
                dst.setFromDate(null);
                dst.setToDate(null);
                dst.setGender(null);
                dst.setEmail(null);
                dst.setAddress(null);
                dst.setCode(icd.getNumber());
                result.add(dst);
            } else {
                log.warn("User has no tax document: transactionId = {}, userRef = {}", transactionId, userRef);
            }
        }

        return result;
    }

    public void notify(String userRef, String templateCode, String resourceId) {
        var req = CreateNotificationWithPushesRequest.newBuilder()
                .setUserIdentifier(userRef)
                .setTemplateCode(templateCode);
        if (resourceId != null) {
            req = req.setResourceId(resourceId);
        }
        notificationService.createNotificationWithPushes(req.build());
    }

    private <T> T safeCall(Supplier<T> f, Consumer<Exception> h) {
        try {
            return f.get();
        } catch (Exception e) {
            h.accept(e);
            return null;
        }
    }

    private String oneOf(String s1, String s2) {
        if ((s1 == null) || (s1.isBlank())) {
            return s2;
        }
        return s1;
    }
}

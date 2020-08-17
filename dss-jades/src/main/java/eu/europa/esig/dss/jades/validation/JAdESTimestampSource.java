package eu.europa.esig.dss.jades.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.crl.CRLUtils;
import eu.europa.esig.dss.enumerations.ArchiveTimestampType;
import eu.europa.esig.dss.enumerations.PKIEncoding;
import eu.europa.esig.dss.enumerations.TimestampLocation;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.enumerations.TimestampedObjectType;
import eu.europa.esig.dss.jades.JAdESArchiveTimestampType;
import eu.europa.esig.dss.jades.JAdESHeaderParameterNames;
import eu.europa.esig.dss.jades.JAdESUtils;
import eu.europa.esig.dss.model.identifier.Identifier;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSRevocationUtils;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificateRef;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLRef;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPRef;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPResponseBinary;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.SignatureProperties;
import eu.europa.esig.dss.validation.timestamp.AbstractTimestampSource;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.dss.validation.timestamp.TimestampedReference;

@SuppressWarnings("serial")
public class JAdESTimestampSource extends AbstractTimestampSource<JAdESAttribute> {

	private static final Logger LOG = LoggerFactory.getLogger(JAdESTimestampSource.class);

	private final JAdESSignature signature;

	public JAdESTimestampSource(JAdESSignature signature) {
		super(signature);
		this.signature = signature;
	}

	@Override
	protected SignatureProperties<JAdESAttribute> getSignedSignatureProperties() {
		return new JAdESSignedProperties(signature.getJws().getHeaders());
	}

	@Override
	protected SignatureProperties<JAdESAttribute> getUnsignedSignatureProperties() {
		List<Object> etsiU = JAdESUtils.getEtsiU(signature.getJws());
		return new JAdESUnsignedProperties(etsiU);
	}

	@Override
	protected boolean isContentTimestamp(JAdESAttribute signedAttribute) {
		return JAdESHeaderParameterNames.ADO_TST.equals(signedAttribute.getHeaderName());
	}

	@Override
	protected boolean isAllDataObjectsTimestamp(JAdESAttribute signedAttribute) {
		// not supported
		return false;
	}

	@Override
	protected boolean isIndividualDataObjectsTimestamp(JAdESAttribute signedAttribute) {
		// not supported
		return false;
	}

	@Override
	protected boolean isSignatureTimestamp(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.SIG_TST.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isCompleteCertificateRef(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.X_REFS.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isAttributeCertificateRef(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.AX_REFS.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isCompleteRevocationRef(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.R_REFS.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isAttributeRevocationRef(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.AR_REFS.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isRefsOnlyTimestamp(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.RFS_TST.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isSigAndRefsTimestamp(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.SIG_AND_RFS_TST.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isCertificateValues(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.X_VALS.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isRevocationValues(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.R_VALS.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isArchiveTimestamp(JAdESAttribute unsignedAttribute) {
		return isArchiveTimestamp(unsignedAttribute.getHeaderName()) && 
				ArchiveTimestampType.JAdES_ALL.equals(getArchiveTimestampType(unsignedAttribute));
	}

	@Override
	protected boolean isPreviousDataArchiveTimestamp(JAdESAttribute unsignedAttribute) {
		return isArchiveTimestamp(unsignedAttribute.getHeaderName()) && 
				ArchiveTimestampType.JAdES_PREVIOUS_ARC_TST.equals(getArchiveTimestampType(unsignedAttribute));
	}
	
	private boolean isArchiveTimestamp(String headerName) {
		return JAdESHeaderParameterNames.ARC_TST.equals(headerName);
	}

	@Override
	protected boolean isTimeStampValidationData(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.TST_VD.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isCounterSignature(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.C_SIG.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected List<TimestampedReference> getIndividualContentTimestampedReferences(JAdESAttribute signedAttribute) {
		// not supported
		return Collections.emptyList();
	}

	@Override
	protected boolean isAttrAuthoritiesCertValues(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.AX_VALS.equals(unsignedAttribute.getHeaderName());
	}

	@Override
	protected boolean isAttributeRevocationValues(JAdESAttribute unsignedAttribute) {
		return JAdESHeaderParameterNames.AR_VALS.equals(unsignedAttribute.getHeaderName());
	}
	
	@Override
	public List<TimestampedReference> getSignatureTimestampReferences() {
		// JAdES SigTst covers only the signature value
		return Arrays.asList(new TimestampedReference(signatureId, TimestampedObjectType.SIGNATURE));
	}

	@Override
	protected List<CertificateRef> getCertificateRefs(JAdESAttribute unsignedAttribute) {
		List<CertificateRef> result = new ArrayList<>();
		List<?> certificateRefsList = (List<?>) unsignedAttribute.getValue();
		if (Utils.isCollectionNotEmpty(certificateRefsList)) {
			for (Object item : certificateRefsList) {
				if (item instanceof Map) {
					CertificateRef certificateRef = JAdESCertificateRefExtractionUtils.createCertificateRef((Map<?, ?>) item);
					if (certificateRef != null) {
						result.add(certificateRef);
					}
				}
			}
		}
		return result;
	}

	@Override
	protected List<CRLRef> getCRLRefs(JAdESAttribute unsignedAttribute) {
		List<CRLRef> result = new ArrayList<>();
		Map<?,?> refsValueMap = (Map<?,?>) unsignedAttribute.getValue();
		List<?> crlRefsList = (List<?>) refsValueMap.get(JAdESHeaderParameterNames.CRL_REFS);
		if (Utils.isCollectionNotEmpty(crlRefsList)) {
			for (Object item : crlRefsList) {
				if (item instanceof Map) {
					CRLRef crlRef = JAdESRevocationRefExtractionUtils.createCRLRef((Map<?, ?>) item);
					if (crlRef != null) {
						result.add(crlRef);
					}
				}
			}
		}
		return result;
	}

	@Override
	protected List<OCSPRef> getOCSPRefs(JAdESAttribute unsignedAttribute) {
		List<OCSPRef> result = new ArrayList<>();
		Map<?,?> refsValueMap = (Map<?,?>) unsignedAttribute.getValue();
		List<?> ocspRefsList = (List<?>) refsValueMap.get(JAdESHeaderParameterNames.OCSP_REFS);
		if (Utils.isCollectionNotEmpty(ocspRefsList)) {
			for (Object item : ocspRefsList) {
				if (item instanceof Map) {
					OCSPRef ocspRef = JAdESRevocationRefExtractionUtils.createOCSPRef((Map<?, ?>) item);
					if (ocspRef != null) {
						result.add(ocspRef);
					}
				}
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<Identifier> getEncapsulatedCertificateIdentifiers(JAdESAttribute unsignedAttribute) {
		List<Object> xVals = null;
		if (isTimeStampValidationData(unsignedAttribute)) {
			Map<String, Object> tstVd = (Map<String, Object>) unsignedAttribute.getValue();
			xVals = (List<Object>) tstVd.get(JAdESHeaderParameterNames.CERT_VALS);
		} else {
			xVals = (List<Object>) unsignedAttribute.getValue();
		}
		if (xVals != null) {
			List<Identifier> certificateIdentifiers = new ArrayList<>();
			
			for (Object encapsulatedCert : xVals) {
				try {
					Map<String, Object> map = (Map<String, Object>) encapsulatedCert;
					Map<String, Object> certObject = (Map<String, Object>) map.get(JAdESHeaderParameterNames.X509_CERT);
					if (certObject == null) {
						certObject = (Map<String, Object>) map.get(JAdESHeaderParameterNames.OTHER_CERT);
					}
					String base64Cert = (String) certObject.get(JAdESHeaderParameterNames.VAL);
					if (base64Cert != null) {
						byte[] binaries = Utils.fromBase64(base64Cert);
						CertificateToken certificateToken = DSSUtils.loadCertificate(binaries);
						certificateIdentifiers.add(certificateToken.getDSSId());
					}
				} catch (Exception e) {
					LOG.warn("An error occurred during parsing a certificate. Reason : {}", e.getMessage(), e);
				}
			}
			
			return certificateIdentifiers;
		}
		return Collections.emptyList();
	}

	@Override
	protected List<TimestampedReference> getArchiveTimestampOtherReferences(TimestampToken timestampToken) {
		// not supported
		return Collections.emptyList();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<Identifier> getEncapsulatedCRLIdentifiers(JAdESAttribute unsignedAttribute) {
		Map<String, Object> rVals = null;
		if (isTimeStampValidationData(unsignedAttribute)) {
			Map<String, Object> tstVd = (Map<String, Object>) unsignedAttribute.getValue();
			rVals = (Map<String, Object>) tstVd.get(JAdESHeaderParameterNames.REV_VALS);
		} else {
			rVals = (Map<String, Object>) unsignedAttribute.getValue();
		}
		if (rVals != null) {
			List<Identifier> crlIdentifiers = new ArrayList<>();
			
			List<Object> crlVals = (List<Object>) rVals.get(JAdESHeaderParameterNames.CRL_VALS);
			
			if (Utils.isCollectionNotEmpty(crlVals)) {
				for (Object encapsulatedCrl : crlVals) {
					try {
						Map<String, Object> map = (Map<String, Object>) encapsulatedCrl;
						String base64Crl = (String) map.get(JAdESHeaderParameterNames.VAL);
						if (base64Crl != null) {
							byte[] binaries = Utils.fromBase64(base64Crl);
							crlIdentifiers.add(CRLUtils.buildCRLBinary(binaries));
						}
					} catch (Exception e) {
						LOG.warn("An error occurred during parsing a CRL. Reason : {}", e.getMessage(), e);
					}
				}
			}
			
			return crlIdentifiers;
		}
		return Collections.emptyList();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<Identifier> getEncapsulatedOCSPIdentifiers(JAdESAttribute unsignedAttribute) {
		Map<String, Object> rVals = null;
		if (isTimeStampValidationData(unsignedAttribute)) {
			Map<String, Object> tstVd = (Map<String, Object>) unsignedAttribute.getValue();
			rVals = (Map<String, Object>) tstVd.get(JAdESHeaderParameterNames.REV_VALS);
		} else {
			rVals = (Map<String, Object>) unsignedAttribute.getValue();
		}
		if (rVals != null) {
			List<Identifier> ocspIdentifiers = new ArrayList<>();
			
			List<Object> ocspVals = (List<Object>) rVals.get(JAdESHeaderParameterNames.OCSP_VALS);
			
			if (Utils.isCollectionNotEmpty(ocspVals)) {
				for (Object encapsulatedOcsp : ocspVals) {
					try {
						Map<String, Object> map = (Map<String, Object>) encapsulatedOcsp;
						String base64Ocps = (String) map.get(JAdESHeaderParameterNames.VAL);
						if (base64Ocps != null) {
							byte[] binaries = Utils.fromBase64(base64Ocps);
							BasicOCSPResp basicOCSPResp = DSSRevocationUtils.loadOCSPFromBinaries(binaries);
							ocspIdentifiers.add(OCSPResponseBinary.build(basicOCSPResp));
						}
					} catch (Exception e) {
						LOG.warn("An error occurred during parsing a CRL. Reason : {}", e.getMessage(), e);
					}
				}
			}
			
			return ocspIdentifiers;
		}
		return Collections.emptyList();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ArchiveTimestampType getArchiveTimestampType(JAdESAttribute unsignedAttribute) {
		/*
		 * 5.3.6.3	Computation of message-imprint
		 * Absence of timeStamped shall be treated as if it is present with value "all".
		 */
		ArchiveTimestampType archiveTimestampType = ArchiveTimestampType.JAdES_ALL;
		
		Map<String, Object> arcTst = (Map<String, Object>) unsignedAttribute.getValue();
		String timestamped = (String) arcTst.get(JAdESHeaderParameterNames.TIMESTAMPED);
		if (timestamped != null) {
			switch (JAdESArchiveTimestampType.forJsonValue(timestamped)) {
				case TIMESTAMPED_ALL:
					return ArchiveTimestampType.JAdES_ALL;
				case TIMESTAMPED_PREVIOUS_ARC_TST:
					return ArchiveTimestampType.JAdES_PREVIOUS_ARC_TST;
				default:
					LOG.warn("Unsupported 'arcTst.timestamped' type found : {}. The default value 'all' will be used.", timestamped);
			}
		}
		
		return archiveTimestampType;
	}

	@Override
	protected JAdESTimestampDataBuilder getTimestampDataBuilder() {
		return new JAdESTimestampDataBuilder(signature);
	}
	
	@Override
	protected List<TimestampedReference> getCounterSignatureReferences(JAdESAttribute unsignedAttribute) {
		List<TimestampedReference> cSigReferences = new ArrayList<>();
		
		Object cSig = unsignedAttribute.getValue();
		if (cSig != null) {
			JAdESSignature counterSignature = JAdESUtils.extractJAdESCounterSignature(cSig, signature);
			if (counterSignature != null) {
				cSigReferences.add(new TimestampedReference(counterSignature.getId(), TimestampedObjectType.SIGNATURE));
				addReferences(cSigReferences, createReferencesForCertificates(counterSignature.getCertificateSource().getCertificates()));
	
				JAdESTimestampSource timestampSource = counterSignature.getTimestampSource();
				addReferences(cSigReferences, timestampSource.getAllSignedDataReferences());
				addReferences(cSigReferences, timestampSource.getEncapsulatedReferences());
				addReferencesFromPreviousTimestamps(cSigReferences, timestampSource.getAllTimestamps());
			}
		}
		
		return cSigReferences;
	}
	
	/**
	 * Returns concatenated data for an ArchiveTimestamp
	 * 
	 * @param canonicalizationMethod {@link String} canonicalization method to use
	 * @param archiveTimestampType {@link JAdESArchiveTimestampType}
	 * @return byte array
	 */
	public byte[] getArchiveTimestampData(String canonicalizationMethod, JAdESArchiveTimestampType archiveTimestampType) {
		return getTimestampDataBuilder().getArchiveTimestampData(canonicalizationMethod, archiveTimestampType);
	}

	@Override
	protected TimestampToken makeTimestampToken(JAdESAttribute signatureAttribute, TimestampType timestampType,
			List<TimestampedReference> references) {
		throw new UnsupportedOperationException("Attribute can contain more than one timestamp");
	}
	
	@Override
	protected List<TimestampToken> makeTimestampTokens(JAdESAttribute signatureAttribute, TimestampType timestampType,
			List<TimestampedReference> references) {
		if (TimestampType.ARCHIVE_TIMESTAMP.equals(timestampType)) {
			return extractArchiveTimestampTokens(signatureAttribute, references);
		} else {
			Object value = signatureAttribute.getValue();
			if (value instanceof Map<?, ?>) {
				return extractTimestampTokens(signatureAttribute, (Map<?, ?>) value, timestampType, references);
			} else {
				LOG.warn("The timestamp container '{}' shall have a map as a value! The entry is skipped.", signatureAttribute.getHeaderName());
				return Collections.emptyList();
			}
		}
	}

	private List<TimestampToken> extractTimestampTokens(JAdESAttribute signatureAttribute, Map<?, ?> tstContainer, TimestampType timestampType,
			List<TimestampedReference> references) {
		List<TimestampToken> result = new LinkedList<TimestampToken>();

		List<?> tokens = (List<?>) tstContainer.get(JAdESHeaderParameterNames.TS_TOKENS);
		for (Object token : tokens) {
			if (token instanceof Map<?, ?>) {
				Map<?, ?> jsonToken = (Map<?, ?>) token;
				String encoding = (String) jsonToken.get(JAdESHeaderParameterNames.ENCODING);
				if (Utils.isStringEmpty(encoding) || Utils.areStringsEqual(PKIEncoding.DER.getUri(), encoding)) {
					String tstBase64 = (String) jsonToken.get(JAdESHeaderParameterNames.VAL);
					try {
						TimestampToken timestampToken = new TimestampToken(Utils.fromBase64(tstBase64), timestampType, references, TimestampLocation.JAdES);
						timestampToken.setHashCode(signatureAttribute.getValueHashCode());
						result.add(timestampToken);
					} catch (Exception e) {
						LOG.error("Unable to parse timestamp '{}'", tstBase64, e);
					}
				} else {
					LOG.warn("Unsupported encoding {}", encoding);
				}
			} else {
				LOG.warn("The 'tsTokens' element shall contain an array of JSON objects! The entry is skipped.");
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<TimestampToken> extractArchiveTimestampTokens(JAdESAttribute signatureAttribute, List<TimestampedReference> references) {
		Map<String, Object> arcTst = (Map<String, Object>) signatureAttribute.getValue();
		Map<String, Object> tstContainer = (Map<String, Object>) arcTst.get(JAdESHeaderParameterNames.TST_CONTAINER);
		
		return extractTimestampTokens(signatureAttribute, tstContainer, TimestampType.ARCHIVE_TIMESTAMP, references);
	}

}
package eu.europa.esig.dss.xades.extension;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.validation.AdvancedSignature;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class XAdESExtensionXToATest extends AbstractXAdESTestExtension {

    @Override
    protected SignatureLevel getOriginalSignatureLevel() {
        return SignatureLevel.XAdES_X;
    }

    @Override
    protected SignatureLevel getFinalSignatureLevel() {
        return SignatureLevel.XAdES_A;
    }

    @Override
    protected void verifySourcesAndDiagnosticData(List<AdvancedSignature> advancedSignatures, DiagnosticData diagnosticData) {
        if (SignatureLevel.XAdES_X.equals(diagnosticData.getFirstSignatureFormat())) {
            super.verifySourcesAndDiagnosticDataWithOrphans(advancedSignatures, diagnosticData);

        } else if (SignatureLevel.XAdES_BASELINE_LTA.equals(diagnosticData.getFirstSignatureFormat())) {
            super.verifySourcesAndDiagnosticData(advancedSignatures, diagnosticData);

        } else {
            fail("Unexpected format " + diagnosticData.getFirstSignatureFormat());
        }
    }

    @Override
    protected void checkFinalLevel(DiagnosticData diagnosticData) {
        assertEquals(SignatureLevel.XAdES_BASELINE_LTA, diagnosticData.getFirstSignatureFormat());
    }

}
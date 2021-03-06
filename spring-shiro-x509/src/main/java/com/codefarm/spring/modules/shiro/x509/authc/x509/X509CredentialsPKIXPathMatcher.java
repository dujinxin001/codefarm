package com.codefarm.spring.modules.shiro.x509.authc.x509;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.CertPathReviewerException;
import org.bouncycastle.x509.ExtendedPKIXBuilderParameters;
import org.bouncycastle.x509.PKIXCertPathReviewer;

/**
 * Matcher to use when you need to do custom PKIX path validation.
 *
 * See http://java.sun.com/javase/6/docs/technotes/guides/security/certpath/CertPathProgGuide.html for reference
 * and http://stackoverflow.com/questions/2457795/x-509-certificate-validation-with-java-and-bouncycastle/2458343
 * for a quick example using Sun API.
 * 
 * This implementation use the BouncyCastle PKIX API as it behave much better and will make CRLs support easily
 * implemented when needed.
 */
public class X509CredentialsPKIXPathMatcher extends
        AbstractX509CredentialsMatcher
{
    
    static
    {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
        {
            Security.addProvider(new BouncyCastleProvider());
            LOGGER.warn("BouncyCastle Provider was not registered, forced registration. That's certainly something you wanna do yourself.");
        }
    }
    
    @Override
    public boolean doX509CredentialsMatch(X509AuthenticationToken token,
            X509AuthenticationInfo info)
    {
        try
        {
            
            ExtendedPKIXBuilderParameters params = new ExtendedPKIXBuilderParameters(
                    info.getGrantedTrustAnchors(), token.getX509CertSelector());
            params.addStore(token.getX509CertChainStore());
            params.setRevocationEnabled(false);
            
            CertPathBuilder pathBuilder = CertPathBuilder.getInstance("PKIX",
                    BouncyCastleProvider.PROVIDER_NAME);
            CertPathBuilderResult result = pathBuilder.build(params);
            
            if (LOGGER.isDebugEnabled())
            {
                PKIXCertPathReviewer reviewer = new PKIXCertPathReviewer(
                        result.getCertPath(), params);
                String certPathEnd = ((X509Certificate) reviewer.getCertPath()
                        .getCertificates()
                        .get(reviewer.getCertPathSize() - 1)).getSubjectX500Principal()
                        .getName();
                LOGGER.debug("A valid ({}) certification path (length: {}) was found for the following certificate: '{}' ending on: '{}'",
                        new Object[] {
                                reviewer.isValidCertPath(),
                                reviewer.getCertPathSize(),
                                token.getX509Certificate()
                                        .getSubjectX500Principal()
                                        .getName(), certPathEnd });
            }
            
            return true;
            
        }
        catch (GeneralSecurityException ex)
        {
            LOGGER.trace("Unable to do credentials matching", ex);
            return false;
        }
        catch (CertPathReviewerException ex)
        {
            LOGGER.trace("Unable to do credentials matching", ex);
            return false;
        }
        
    }
    
}

package com.ds.tools.hudson.crowd;

import com.atlassian.crowd.integration.acegi.CrowdSSOAuthenticationDetails;
import com.atlassian.crowd.integration.acegi.RemoteCrowdAuthenticationProvider;
import com.atlassian.crowd.integration.acegi.user.CrowdUserDetailsService;
import com.atlassian.crowd.integration.http.HttpAuthenticator;
import com.atlassian.crowd.service.AuthenticationManager;
import com.atlassian.crowd.service.client.ClientProperties;
import org.acegisecurity.providers.AbstractAuthenticationToken;

/**
 * Overridden RemoteCrowdAuthenticationProvider to address what I think is an issue
 * with how the default application name is handled
 */
public class HudsonCrowdAuthenticationProvider extends RemoteCrowdAuthenticationProvider {

    private ClientProperties clientProperties;

    public HudsonCrowdAuthenticationProvider(AuthenticationManager authenticationManager, HttpAuthenticator httpAuthenticator, CrowdUserDetailsService userDetailsService) {
        super(authenticationManager, httpAuthenticator, userDetailsService);
    }

    public void setClientProperties(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Override
    public boolean supports(AbstractAuthenticationToken authenticationToken) {
        if (authenticationToken.getDetails() == null || !(authenticationToken.getDetails() instanceof CrowdSSOAuthenticationDetails))
        {
            // support all non-SSO authentication requests (for compatibility)
            return true;
        }
        else if (authenticationToken.getDetails() instanceof CrowdSSOAuthenticationDetails)
        {
            // support SSO requests that are only requests to the defaultApplication
            CrowdSSOAuthenticationDetails details = (CrowdSSOAuthenticationDetails) authenticationToken.getDetails();
            return details.getApplicationName().equals(clientProperties.getApplicationName());
        }
        else
        {
            return false;
        }
    }
}

import com.atlassian.crowd.integration.acegi.user.CrowdUserDetailsServiceImpl
import org.acegisecurity.ui.webapp.AuthenticationProcessingFilter
import com.atlassian.crowd.integration.acegi.RemoteCrowdAuthenticationProvider
import com.atlassian.crowd.integration.acegi.CrowdSSOAuthenticationProcessingFilter
import hudson.model.Hudson

import org.acegisecurity.providers.ProviderManager
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationProvider
import org.acegisecurity.providers.rememberme.RememberMeAuthenticationProvider

crowdUserDetailsService(CrowdUserDetailsServiceImpl) {
    authenticationManager = ref("crowdAuthenticationManager")
    groupMembershipManager = ref("crowdGroupMembershipManager")
    userManager = ref("crowdUserManager")
}

crowdAuthenticationProvider(RemoteCrowdAuthenticationProvider, ref("crowdAuthenticationManager"), ref("httpAuthenticator"), ref("crowdUserDetailsService")) {
}

authenticationManager(ProviderManager) {
    providers = [
        crowdAuthenticationProvider,

    	// these providers apply everywhere
        bean(RememberMeAuthenticationProvider) {
            key = Hudson.getInstance().getSecretKey();
        },
        // this doesn't mean we allow anonymous access.
        // we just authenticate anonymous users as such,
        // so that later authorization can reject them if so configured
        bean(AnonymousAuthenticationProvider) {
            key = "anonymous"
        }
    ]
}

crowdFilter(AuthenticationProcessingFilter) {
    authenticationFailureUrl = "/loginError"
    defaultTargetUrl = "/"
    filterProcessesUrl = "/j_acegi_security_check"
    authenticationManager = ref("authenticationManager")
}


crowdSSOFilter(CrowdSSOAuthenticationProcessingFilter) {
    authenticationFailureUrl = "/loginError"
    defaultTargetUrl = "/"
    filterProcessesUrl = "/j_acegi_security_check"
    authenticationManager = ref("authenticationManager")
    httpAuthenticator = ref("httpAuthenticator")
}

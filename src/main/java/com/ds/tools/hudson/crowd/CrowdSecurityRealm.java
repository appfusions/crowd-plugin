package com.ds.tools.hudson.crowd;

import groovy.lang.Binding;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.security.ChainedServletFilter;
import hudson.security.SecurityRealm;
import hudson.util.spring.BeanBuilder;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.ui.webapp.AuthenticationProcessingFilter;
import org.acegisecurity.userdetails.UserDetailsService;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;

import static org.fest.reflect.core.Reflection.field;

public class CrowdSecurityRealm extends SecurityRealm {
    private static org.apache.log4j.Logger log = Logger.getLogger(CrowdSecurityRealm.class);

    private transient WebApplicationContext crowdGroovyContext;
    public final String url;
    public final String applicationName;
    public final String applicationPassword;
    public final boolean ssoEnabled;

    @Extension
    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {
        public DescriptorImpl() {
            super(CrowdSecurityRealm.class);
        }

        @Override
        public String getDisplayName() {
            return "Crowd";
        }
    }

    @DataBoundConstructor
    public CrowdSecurityRealm(String url, String applicationName, String applicationPassword, boolean ssoEnabled) {
        this.url = url.trim();
        this.applicationName = applicationName.trim();
        this.applicationPassword = applicationPassword.trim();
        this.ssoEnabled = ssoEnabled;
    }

    public SecurityComponents createSecurityComponents() {
        // load the base configuration from the crowd-integration-client jar
        XmlWebApplicationContext crowdConfigContext = new XmlWebApplicationContext();
        crowdConfigContext.setClassLoader(getClass().getClassLoader());
        crowdConfigContext.setConfigLocations(new String[]{"classpath:/applicationContext-CrowdClient.xml"});

        crowdConfigContext.refresh();

        // load the Hudson-Crowd configuration from Crowd.groovy
        BeanBuilder builder = new BeanBuilder(crowdConfigContext, getClass().getClassLoader());
        Binding binding = new Binding();
        builder.parse(getClass().getResourceAsStream("Crowd.groovy"), binding);
        crowdGroovyContext = builder.createApplicationContext();

//        // configure the ClientProperties object
//        if (applicationName != null || applicationPassword != null || url != null) {
//            Properties props = new Properties();
//            props.setProperty("application.name", applicationName);
//            props.setProperty("application.password", applicationPassword);
//            props.setProperty("crowd.server.url", url);
//            props.setProperty("session.validationinterval", "5");
//            ClientProperties clientProperties = (ClientProperties) crowdConfigContext.getBean("clientProperties");
//            clientProperties.updateProperties(props);
//
//            HudsonCrowdAuthenticationProvider provider = (HudsonCrowdAuthenticationProvider)crowdGroovyContext.getBean("crowdAuthenticationProvider");
//            provider.setClientProperties(clientProperties);
//        } else {
//            log.warn("Client properties are incomplete");
//        }

        return new SecurityComponents(findBean(AuthenticationManager.class, crowdGroovyContext), findBean(UserDetailsService.class, crowdGroovyContext));
    }

    @Override
    public Filter createFilter(FilterConfig filterConfig) {
        Filter chainedFilter = super.createFilter(filterConfig);

        AuthenticationProcessingFilter crowdFilter = (AuthenticationProcessingFilter)crowdGroovyContext.getBean(getCrowdBeanId());
        crowdFilter.setRememberMeServices(getSecurityComponents().rememberMe);

        return replaceHudsonBasicAuthenticationFilter(chainedFilter, crowdFilter);
    }

    public String getCrowdBeanId() {
         return (ssoEnabled ? "crowdSSOFilter" : "crowdFilter");
     }

    private Filter replaceHudsonBasicAuthenticationFilter(Filter filter, AuthenticationProcessingFilter crowdFilter) {
        if (!(filter instanceof ChainedServletFilter)) {
            log.error("Expected to insert the crowd filter into a chained filter but it wasn't there, so just using crowd");
            return crowdFilter;
        }

        ChainedServletFilter chainedFilter = (ChainedServletFilter)filter;

        boolean found = false;
        Filter[] filters = field("filters").ofType(Filter[].class).in(chainedFilter).get();
        for (int i=0; i < filters.length; i++) {
            if (filters[i] instanceof AuthenticationProcessingFilter) {
                filters[i] = crowdFilter;
                found = true;
                break;
            }
        }


        if (!found) {
            log.warn("Could not find a Filter of instance AuthenticationProcessingFilter to replace with a Crowd one");
        }

        return chainedFilter;
    }
}

package com.jn.esmvc.service.rest.config;

import com.jn.langx.util.Emptys;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClientBuilder;


public class DefaultRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

    private EsmvcRestClientProperties esmvcRestClientProperties;

    public DefaultRestClientBuilderCustomizer(){}

    public DefaultRestClientBuilderCustomizer(EsmvcRestClientProperties restClientProperties) {
        this.esmvcRestClientProperties = restClientProperties;
    }

    @Override
    public void customize(RestClientBuilder builder) {
    }

    @Override
    public void customize(HttpAsyncClientBuilder builder) {
        String username = esmvcRestClientProperties.getUsername();
        if (Emptys.isNotEmpty(username)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            String password = esmvcRestClientProperties.getPassword();
            Credentials credentials = new UsernamePasswordCredentials(username, password);
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
    }



    @Override
    public void customize(RequestConfig.Builder builder) {
        builder.setConnectTimeout(esmvcRestClientProperties.getConnectTimeout());
        builder.setSocketTimeout(esmvcRestClientProperties.getReadTimeout());
    }

    public void setEsmvcRestClientProperties(EsmvcRestClientProperties esmvcRestClientProperties) {
        this.esmvcRestClientProperties = esmvcRestClientProperties;
    }
}

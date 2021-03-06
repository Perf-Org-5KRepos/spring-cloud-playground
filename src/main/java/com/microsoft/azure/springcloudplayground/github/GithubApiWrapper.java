package com.microsoft.azure.springcloudplayground.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.springcloudplayground.exception.GithubFileException;
import com.microsoft.azure.springcloudplayground.exception.GithubProcessException;
import com.microsoft.azure.springcloudplayground.github.gitdata.GitDataRequestBlob;
import com.microsoft.azure.springcloudplayground.github.gitdata.GitDataRequestCommit;
import com.microsoft.azure.springcloudplayground.github.gitdata.GitDataRequestReference;
import com.microsoft.azure.springcloudplayground.github.gitdata.GitDataRequestTree;
import lombok.Getter;
import lombok.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GithubApiWrapper {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String AUTH_HEADER = "Authorization";

    private static final String ACCEPT_HEADER = "Accept";

    private static final String ACCEPT_VALUE = "application/vnd.github.v3.full+json";

    private static final String CREATE_REPOSITORY_URL = "https://api.github.com/user/repos";

    private static final String USER_EMAILS_URL = "https://api.github.com/user/emails";

    static {
        MAPPER.configure(MapperFeature.AUTO_DETECT_FIELDS, false);
    }

    @Getter
    private final String token;
    @Getter
    private final String username;

    protected GithubApiWrapper(@NonNull String username, @NonNull String token) {
        this.username = username;
        this.token = token;
    }

    private HttpResponse executeRequest(@NonNull HttpUriRequest request) throws GithubProcessException {
        try {
            request.setHeader(ACCEPT_HEADER, ACCEPT_VALUE);
            HttpClient client = HttpClientBuilder.create().build();

            return client.execute(request);
        } catch (IOException e) {
            throw new GithubProcessException("Failed to execute request to github api", e);
        }
    }

    protected String getContent(@NonNull HttpResponse response) throws GithubProcessException {
        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder builder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }

            return builder.toString();
        } catch (IOException e) {
            throw new GithubProcessException("Failed to obtain github api response content", e);
        }
    }

    protected <T> T readValue(@NonNull String json, Class<T> clazz) throws GithubProcessException {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new GithubProcessException("Failed to retrieve object from Json", e);
        }
    }

    private void appendAuthorizationHeader(@NonNull HttpUriRequest request) {
        request.setHeader(AUTH_HEADER, String.format("token %s", this.token));
    }

    protected HttpResponse getUserEmails() throws GithubProcessException {
        HttpGet request = new HttpGet(USER_EMAILS_URL);

        appendAuthorizationHeader(request);

        return executeRequest(request);
    }

    protected HttpResponse createRepository(@NonNull GithubRepository repository) throws GithubProcessException {
        HttpPost request = new HttpPost(CREATE_REPOSITORY_URL);

        try {
            StringEntity body = new StringEntity(MAPPER.writeValueAsString(repository), ContentType.APPLICATION_JSON);
            request.setEntity(body);
        } catch (JsonProcessingException e) {
            throw new GithubFileException("Failed to read GithubRepository to Json", e);
        }

        appendAuthorizationHeader(request);

        return executeRequest(request);
    }

    protected HttpResponse deleteRepository(@NonNull String repositoryName) throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s";
        HttpDelete request = new HttpDelete(String.format(url, username, repositoryName));

        appendAuthorizationHeader(request);

        return executeRequest(request);
    }

    protected HttpResponse getAllCommits(@NonNull String repositoryName) throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s/commits";
        HttpGet request = new HttpGet(String.format(url, username, repositoryName));

        return executeRequest(request);
    }

    protected HttpResponse getGitDataCommit(@NonNull String repositoryName, @NonNull String commitSha)
            throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s/git/commits/%s";
        HttpGet request = new HttpGet(String.format(url, username, repositoryName, commitSha));

        return executeRequest(request);
    }

    protected HttpResponse getGitDataTree(@NonNull String repositoryName, @NonNull String treeSha)
            throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s/git/trees/%s";
        HttpGet request = new HttpGet(String.format(url, username, repositoryName, treeSha));

        return executeRequest(request);
    }

    protected HttpResponse createGitDataTree(@NonNull String repositoryName, @NonNull GitDataRequestTree tree)
            throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s/git/trees";
        HttpPost request = new HttpPost(String.format(url, username, repositoryName));

        try {
            StringEntity body = new StringEntity(MAPPER.writeValueAsString(tree), ContentType.APPLICATION_JSON);
            request.setEntity(body);
        } catch (JsonProcessingException e) {
            throw new GithubProcessException("Failed to read GitDataRequestTree to Json", e);
        }

        appendAuthorizationHeader(request);

        return executeRequest(request);
    }

    protected HttpResponse createGitDataBlob(@NonNull String repositoryName, @NonNull GitDataRequestBlob blob)
            throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s/git/blobs";
        HttpPost request = new HttpPost(String.format(url, username, repositoryName));

        try {
            StringEntity body = new StringEntity(MAPPER.writeValueAsString(blob), ContentType.APPLICATION_JSON);
            request.setEntity(body);
        } catch (JsonProcessingException e) {
            throw new GithubProcessException("Failed to read GitDataRequestBlob to Json", e);
        }

        appendAuthorizationHeader(request);

        return executeRequest(request);
    }

    protected HttpResponse createGitDataCommit(@NonNull String repositoryName, @NonNull GitDataRequestCommit commit)
            throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s/git/commits";
        HttpPost request = new HttpPost(String.format(url, username, repositoryName));

        try {
            StringEntity body = new StringEntity(MAPPER.writeValueAsString(commit), ContentType.APPLICATION_JSON);
            request.setEntity(body);
        } catch (JsonProcessingException e) {
            throw new GithubProcessException("Failed to read GitDataRequestCommit to Json", e);
        }

        appendAuthorizationHeader(request);

        return executeRequest(request);
    }

    protected HttpResponse updateGitDataReference(@NonNull String repositoryName,
                                                  @NonNull GitDataRequestReference reference)
            throws GithubProcessException {
        String url = "https://api.github.com/repos/%s/%s/git/refs/heads/master";
        HttpPatch request = new HttpPatch(String.format(url, username, repositoryName));

        try {
            StringEntity body = new StringEntity(MAPPER.writeValueAsString(reference), ContentType.APPLICATION_JSON);
            request.setEntity(body);
        } catch (JsonProcessingException e) {
            throw new GithubProcessException("Failed to read GitDataRequestReference to Json", e);
        }

        appendAuthorizationHeader(request);

        return executeRequest(request);
    }
}

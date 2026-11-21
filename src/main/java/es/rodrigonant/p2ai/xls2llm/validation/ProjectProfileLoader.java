package es.rodrigonant.p2ai.xls2llm.validation;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProjectProfileLoader {
    private static final String DEFAULT_PROFILE = "classpath:profiles/nordeste-eleicoes-2022/1.0.0.json";

    private final ProjectProfile profile;

    @Autowired
    public ProjectProfileLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper,
			@Value("${xls2llm.profile:classpath:profiles/nordeste-eleicoes-2022/1.0.0.json}") String profilePath) {
        try {
            Resource resource = resourceLoader.getResource(profilePath == null || profilePath.isBlank() ? DEFAULT_PROFILE : profilePath);
            try (InputStream stream = resource.getInputStream()) {
                this.profile = objectMapper.readValue(stream, ProjectProfile.class);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load the default project profile", e);
        }
    }

    public ProjectProfileLoader(ProjectProfile profile) {
        this.profile = profile;
    }

    public ProjectProfile getProfile() {
        return profile;
    }
}

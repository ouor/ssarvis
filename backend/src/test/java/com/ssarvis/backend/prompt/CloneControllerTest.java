package com.ssarvis.backend.prompt;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.api.GlobalExceptionHandler;
import com.ssarvis.backend.access.AssetListScope;
import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CloneController.class)
@Import(GlobalExceptionHandler.class)
class CloneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptService promptService;

    @Test
    void listClonesReturnsMineScope() throws Exception {
        given(promptService.listClones(1L, AssetListScope.MINE)).willReturn(List.of(
                new CloneSummaryResponse(11L, Instant.parse("2026-03-26T02:00:00Z"), "미소", "미소 설명", false, "하루"),
                new CloneSummaryResponse(10L, Instant.parse("2026-03-26T01:00:00Z"), "하루", "하루 설명", false, "하루")
        ));

        mockMvc.perform(get("/api/clones")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cloneId").value(11))
                .andExpect(jsonPath("$[0].alias").value("미소"))
                .andExpect(jsonPath("$[0].isPublic").value(false))
                .andExpect(jsonPath("$[1].cloneId").value(10))
                .andExpect(jsonPath("$[1].alias").value("하루"));
    }

    @Test
    void listClonesReturnsPublicScope() throws Exception {
        given(promptService.listClones(1L, AssetListScope.PUBLIC)).willReturn(List.of(
                new CloneSummaryResponse(12L, Instant.parse("2026-03-26T03:00:00Z"), "공개 클론", "공개 설명", true, "미소")
        ));

        mockMvc.perform(get("/api/clones")
                        .param("scope", "public")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cloneId").value(12))
                .andExpect(jsonPath("$[0].isPublic").value(true))
                .andExpect(jsonPath("$[0].ownerDisplayName").value("미소"));
    }

    @Test
    void updateCloneVisibilityReturnsUpdatedState() throws Exception {
        given(promptService.updateCloneVisibility(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(12L), org.mockito.ArgumentMatchers.any()))
                .willReturn(new CloneVisibilityResponse(12L, true));

        mockMvc.perform(patch("/api/clones/12/visibility")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        )
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isPublic": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloneId").value(12))
                .andExpect(jsonPath("$.isPublic").value(true));
    }
}

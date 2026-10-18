// 閱讀導引：驗證活動圖片的處理與管理流程，包含不合法輸入的處理。
package com.demo.ticket;

import com.demo.ticket.Dto.Admin.AdminDeleteActivityRequest;
import com.demo.ticket.Dto.Admin.AdminSaveActivityRequest;
import com.demo.ticket.Exception.FieldValidationException;
import com.demo.ticket.Mapper.AdminMapper;
import com.demo.ticket.Service.Admin.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminActivityImageTests {

    private AdminMapper mapper;
    private AdminServiceImpl service;
    private UUID activityUuid;

    /**
     * 為每個測試初始化模擬元件與共用資料，避免測試情境互相影響。
     */
    @BeforeEach
    void setup() {
        mapper = mock(AdminMapper.class);
        service = new AdminServiceImpl(mapper);
        activityUuid = UUID.randomUUID();
        when(mapper.create_activity(any())).thenReturn(Map.of(
                "activity_id", "ACT-20260917-001",
                "activity_uuid", activityUuid));
        when(mapper.selectAllActivities()).thenReturn(List.of());
        when(mapper.selectOnlyActivities("ACT-20260917-001")).thenReturn(Map.of(
                "ACT-20260917-001", Map.of("id", "ACT-20260917-001",
                        "image_url", "/v1/activity/activityImage/ACT-20260917-001")));
    }

    /**
     * 驗證圖片儲存為縮放後 JPEG，且記錄的尺寸與實際圖片一致。
     */
    @Test
    void savesResizedJpegWithMatchingDimensions() throws Exception {
        BufferedImage original = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream upload = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(original, "JPEG", upload));
        MockMultipartFile image = new MockMultipartFile(
                "image", "poster.jpg", "image/jpeg", upload.toByteArray());

        Map<String, Object> result = service.saveActivity(request(), image);
        assertEquals("/v1/activity/activityImage/ACT-20260917-001", result.get("image_url"));
        assertFalse(result.containsKey("image_data"));

        ArgumentCaptor<Integer> width = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> height = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(mapper).upsertActivityImage(eq(activityUuid), eq(activityUuid + ".jpg"),
                width.capture(), height.capture(), bytes.capture());
        assertEquals(1280, width.getValue());
        assertEquals(640, height.getValue());
        assertTrue(bytes.getValue().length <= 1024 * 1024);
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(bytes.getValue()));
        assertNotNull(stored);
        assertEquals(width.getValue().intValue(), stored.getWidth());
        assertEquals(height.getValue().intValue(), stored.getHeight());
    }

    /**
     * 驗證未重新選圖時不覆寫原有活動圖片。
     */
    @Test
    void leavesExistingImageUntouchedWhenNoImageIsSelected() {
        service.saveActivity(request(), null);

        verify(mapper, never()).upsertActivityImage(any(), anyString(), anyInt(), anyInt(), any());
    }

    /**
     * 驗證活動類別不允許的排數會被拒絕。
     */
    @Test
    void rejectsSeatRowsOutsideTheActivityCategory() {
        AdminSaveActivityRequest request = new AdminSaveActivityRequest(
                "", "測試活動", "SPECIAL_EXHIBITION", "測試場地",
                BigDecimal.valueOf(1280), "", "EU", BigDecimal.TEN);

        assertThrows(FieldValidationException.class, () -> service.saveActivity(request, null));
        verify(mapper, never()).create_activity(any());
    }

    /**
     * 驗證能建立活動類別允許上限內的座位排數。
     */
    @Test
    void createsRowsThroughTheCategoryLimit() {
        service.saveActivity(request("MUSIC_CONCERT", "AX"), null);
        service.saveActivity(request("STAGE_PLAY", "CV"), null);
        service.saveActivity(request("SPECIAL_EXHIBITION", "ET"), null);

        ArgumentCaptor<String> rows = ArgumentCaptor.forClass(String.class);
        verify(mapper, times(3)).create_seat(anyString(), anyString(), rows.capture(), eq(BigDecimal.TEN));
        assertEquals(List.of(50, 100, 150), rows.getAllValues().stream()
                .map(value -> value.split(", ").length).toList());
        assertEquals(List.of("AX", "CV", "ET"), rows.getAllValues().stream()
                .map(value -> value.substring(value.lastIndexOf(", ") + 2)).toList());
    }

    /**
     * 驗證刪除活動時先移除圖片，維持刪除順序。
     */
    @Test
    void deletesImageBeforeItsActivity() {
        when(mapper.lockActivity("ACT-20260917-001")).thenReturn("ACT-20260917-001");
        when(mapper.delete_activity("ACT-20260917-001")).thenReturn(1);

        ResponseEntity<?> response = service.deleteActivity(new AdminDeleteActivityRequest("ACT-20260917-001"));
        assertEquals(org.springframework.http.HttpStatus.OK, response.getStatusCode());

        InOrder calls = inOrder(mapper);
        calls.verify(mapper).lockActivity("ACT-20260917-001");
        calls.verify(mapper).activityHasSessions("ACT-20260917-001");
        calls.verify(mapper).deleteActivityImage("ACT-20260917-001");
        calls.verify(mapper).delete_seat_by_activity("ACT-20260917-001");
        calls.verify(mapper).delete_activity("ACT-20260917-001");
    }

    /**
     * 建立活動管理測試所需的請求資料，供不同類別與排數情境共用。
     */
    private AdminSaveActivityRequest request() {
        return request("MUSIC_CONCERT", "B");
    }

    /**
     * 建立活動管理測試所需的請求資料，供不同類別與排數情境共用。
     */
    private AdminSaveActivityRequest request(String category, String lastRow) {
        return new AdminSaveActivityRequest(
                "", "測試活動", category, "測試場地",
                BigDecimal.valueOf(1280), "", lastRow, BigDecimal.TEN);
    }
}

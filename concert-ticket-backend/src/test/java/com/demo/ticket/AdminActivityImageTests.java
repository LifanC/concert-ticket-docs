package com.demo.ticket;

import com.demo.ticket.Dto.Admin.AdminSaveActivityRequest;
import com.demo.ticket.Dto.Admin.AdminDeleteActivityRequest;
import com.demo.ticket.Mapper.AdminMapper;
import com.demo.ticket.Service.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminActivityImageTests {

    private AdminMapper mapper;
    private AdminServiceImpl service;
    private UUID activityUuid;

    @BeforeEach
    void setup() {
        mapper = mock(AdminMapper.class);
        service = new AdminServiceImpl(mapper);
        activityUuid = UUID.randomUUID();
        when(mapper.create_activity(any())).thenReturn(Map.of(
                "activity_id", "ACT-20260917-001",
                "activity_uuid", activityUuid));
        when(mapper.selectAllActivities()).thenReturn(List.of());
    }

    @Test
    void savesResizedJpegWithMatchingDimensions() throws Exception {
        BufferedImage original = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream upload = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(original, "JPEG", upload));
        MockMultipartFile image = new MockMultipartFile(
                "image", "poster.jpg", "image/jpeg", upload.toByteArray());

        service.saveActivity(request(), image);

        var width = org.mockito.ArgumentCaptor.forClass(Integer.class);
        var height = org.mockito.ArgumentCaptor.forClass(Integer.class);
        var bytes = org.mockito.ArgumentCaptor.forClass(byte[].class);
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

    @Test
    void leavesExistingImageUntouchedWhenNoImageIsSelected() {
        service.saveActivity(request(), null);

        verify(mapper, never()).upsertActivityImage(any(), anyString(), anyInt(), anyInt(), any());
    }

    @Test
    void readsStoredImageForEditing() {
        byte[] jpeg = { (byte) 0xff, (byte) 0xd8, (byte) 0xff };
        when(mapper.selectActivityImage("ACT-20260917-001"))
                .thenReturn(Map.of("image_data", jpeg));

        var response = service.activityImage("ACT-20260917-001");

        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertArrayEquals(jpeg, response.getBody());
    }

    @Test
    void deletesImageBeforeItsActivity() {
        service.deleteActivity(new AdminDeleteActivityRequest("ACT-20260917-001"));

        var calls = inOrder(mapper);
        calls.verify(mapper).deleteActivityImage("ACT-20260917-001");
        calls.verify(mapper).delete_activity("ACT-20260917-001");
    }

    private AdminSaveActivityRequest request() {
        return new AdminSaveActivityRequest(
                "", "測試活動", "MUSIC_CONCERT", "測試場地",
                BigDecimal.valueOf(1280), "", "AB", BigDecimal.TEN);
    }
}

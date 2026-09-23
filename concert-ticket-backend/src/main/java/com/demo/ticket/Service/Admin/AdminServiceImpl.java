package com.demo.ticket.Service.Admin;

import com.demo.ticket.Dto.Admin.*;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Exception.FieldValidationException;
import com.demo.ticket.Mapper.AdminMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AdminServiceImpl implements AdminService{

    private static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
    private static final int MAX_STORED_BYTES = 1024 * 1024;
    private static final int MAX_IMAGE_SIDE = 1280;
    private static final long MAX_SOURCE_PIXELS = 24_000_000L;
    private static final float[] JPEG_QUALITIES = {0.85f, 0.75f, 0.65f, 0.55f, 0.45f};

    private final AdminMapper adminMapper;

    public AdminServiceImpl(
            AdminMapper adminMapper
    ) {
        this.adminMapper = adminMapper;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllActivities() {
        return adminMapper.selectAllActivities();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllSessions() {
        return adminMapper.selectAllSessions();
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllticket() {
        return adminMapper.selectAllticket();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public Map<String, Object> saveActivity(AdminSaveActivityRequest request, MultipartFile image) {
        final String id = request.id() == null ? "" : request.id().trim();
        final String name = request.name().trim();
        final String category = request.category().trim();
        final String venue = request.venue().trim();
        final BigDecimal price = request.price();
        final String description = request.description() == null ? "" : request.description().trim();
        final String column = request.column().trim();
        final BigDecimal row = request.row();
        int selectedRows = getSelectedRows(category, column);
        final String seat_id = column + "-" + row.toPlainString();
        PreparedImage preparedImage = prepareImage(image);
        Activity activity = new Activity();
        activity.setId(id);
        activity.setName(name);
        activity.setCategory(category);
        activity.setVenue(venue);
        activity.setPrice(price);
        activity.setDescription(description);
        Map<String, Object> savedActivity = adminMapper.create_activity(activity);
        String activity_id = savedActivity.get("activity_id").toString();
        StringJoiner result = new StringJoiner(", ");
        for (int number = 1; number <= selectedRows; number++) {
            result.add(seatRowLabel(number));
        }
        adminMapper.delete_seat_by_activity(activity_id);
        adminMapper.create_seat(seat_id, activity_id, result.toString(), row);
        if (preparedImage != null) {
            UUID activity_uuid = UUID.fromString(savedActivity.get("activity_uuid").toString());
            adminMapper.upsertActivityImage(
                    activity_uuid,
                    activity_uuid + ".jpg",
                    preparedImage.width(),
                    preparedImage.height(),
                    preparedImage.data()
            );
        }
        return adminMapper.selectOnlyActivities(activity_id).get(activity_id);
    }

    private int getSelectedRows(String category, String column) {
        final int maxRows = switch (category) {
            case "MUSIC_CONCERT" -> 50;
            case "STAGE_PLAY" -> 100;
            case "SPECIAL_EXHIBITION" -> 150;
            default -> throw new FieldValidationException("category", "活動類型錯誤");
        };
        int selectedRows = 0;
        for (char letter : column.toCharArray()) {
            selectedRows = selectedRows * 26 + letter - 'A' + 1;
        }
        if (selectedRows > maxRows) {
            throw new FieldValidationException("column", "座位排別不符合活動類型");
        }
        return selectedRows;
    }

    private String seatRowLabel(int number) {
        StringBuilder label = new StringBuilder();
        while (number > 0) {
            number--;
            label.insert(0, (char) ('A' + number % 26));
            number /= 26;
        }
        return label.toString();
    }

    private PreparedImage prepareImage(MultipartFile image) {
        if (image == null) return null;
        if (image.isEmpty() || image.getSize() > MAX_UPLOAD_BYTES) {
            throw new FieldValidationException("image", "請選擇小於 10 MB 的 JPG 圖片");
        }
        if (!"image/jpeg".equalsIgnoreCase(image.getContentType())) {
            throw new FieldValidationException("image", "只接受 JPG 圖片");
        }
        try {
            BufferedImage source;
            try (ImageInputStream input = ImageIO.createImageInputStream(
                    new ByteArrayInputStream(image.getBytes()))) {
                if (input == null) {
                    throw new FieldValidationException("image", "無法讀取圖片");
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw new FieldValidationException("image", "圖片內容格式錯誤");
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    if (!"JPEG".equalsIgnoreCase(reader.getFormatName())) {
                        throw new FieldValidationException("image", "圖片內容必須是 JPEG");
                    }
                    int sourceWidth = reader.getWidth(0);
                    int sourceHeight = reader.getHeight(0);
                    if (sourceWidth < 1 || sourceHeight < 1 ||
                            (long) sourceWidth * sourceHeight > MAX_SOURCE_PIXELS) {
                        throw new FieldValidationException("image", "圖片像素過大，最多 2400 萬像素");
                    }
                    source = reader.read(0);
                } finally {
                    reader.dispose();
                }
            }
            double scale = Math.min(1.0, (double) MAX_IMAGE_SIDE / Math.max(source.getWidth(), source.getHeight()));
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            for (int attempt = 0; attempt < 12; attempt++) {
                BufferedImage resized = resizeImage(source, width, height);
                for (float quality : JPEG_QUALITIES) {
                    byte[] encoded = encodeJpeg(resized, quality);
                    if (encoded.length <= MAX_STORED_BYTES) {
                        return new PreparedImage(width, height, encoded);
                    }
                }
                width = Math.max(1, (int) Math.round(width * 0.8));
                height = Math.max(1, (int) Math.round(height * 0.8));
            }
            throw new FieldValidationException("image", "圖片無法壓縮到 1 MB 以下");
        } catch (IOException ex) {
            throw new FieldValidationException("image", "JPG 圖片已損壞或無法讀取", ex);
        }
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            throw new IOException("找不到 JPEG 編碼器");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> deleteActivity(AdminDeleteActivityRequest request) {
        final String id = request.id().trim();
        int[] cnts = {
                adminMapper.deleteActivityImage(id),
                adminMapper.delete_activity(id)
        };
        if (Arrays.stream(cnts).allMatch(cnt -> cnt > 0)) {
            adminMapper.delete_seat_by_activity(id);
        }
        List<Map<String, Object>> data = adminMapper.selectAllActivities();
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> createSession(AdminCreateSessionRequest request) {
        final String id = request.id().trim();
        final String activity_id = request.activity_id().trim();
        final String date = request.date().trim();
        final String time = request.time().trim();
        final String salesdate = request.salesdate().trim();
        final String salestime = request.salestime().trim();
        final String statusSession = request.status().trim();
        Session session = new Session();
        session.setId(id);
        session.setActivity_id(activity_id);
        session.setDate(date);
        session.setTime(time);
        session.setSalesdate(salesdate);
        session.setSalestime(salestime);
        BigDecimal capacity = BigDecimal.ZERO;
        Map<String, Object> dataMapOnlySeats = adminMapper.selectOnlySeats(activity_id).get(activity_id);
        if (dataMapOnlySeats != null) {
            int rows = dataMapOnlySeats.get("seat_rows").toString().split(",").length;
            int seatsPerRow = Integer.parseInt(dataMapOnlySeats.get("seats_per_row").toString());
            capacity = BigDecimal.valueOf((long) rows * seatsPerRow);
        }
        session.setCapacity(capacity);
        session.setStatus(statusSession);
        adminMapper.create_session(session);
        List<Map<String, Object>> data = adminMapper.selectAllSessions();
        HttpStatus status = HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.api(
                        status,
                        data
                ));
    }

}

// 閱讀導引：管理活動、圖片與場次；儲存前檢查資料及圖片，並透過交易維持資料一致性。
package com.demo.ticket.Service.Admin;

import com.demo.ticket.Dto.Admin.*;
import com.demo.ticket.Dto.ApiResponse;
import com.demo.ticket.Exception.FieldValidationException;
import com.demo.ticket.Mapper.AdminMapper;
import com.demo.ticket.Config.WebSocket.NotificationMessage;
import com.demo.ticket.Config.WebSocket.NotifierConsumer;
import com.demo.ticket.security.LoginUser;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Resource
    private NotifierConsumer notifierConsumer;

    private final AdminMapper adminMapper;

    /**
     * 注入活動、圖片及場次的資料存取介面；通知元件另由 Spring 注入。
     */
    public AdminServiceImpl(
            AdminMapper adminMapper
    ) {
        this.adminMapper = adminMapper;
    }

    /**
     * 查詢活動清單，回傳活動資訊供頁面呈現。
     */
    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllActivities() {
        return adminMapper.selectAllActivities();
    }

    /**
     * 查詢管理頁使用的場次清單及售票資訊。
     */
    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllSessions() {
        return adminMapper.selectAllSessions();
    }

    /**
     * 查詢管理頁使用的訂單清單。
     */
    @Override
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public List<Map<String, Object>> selectAllticket() {
        return adminMapper.selectAllticket();
    }

    /**
     * 依 id 新增或更新活動，同時重建活動的排號與每排座位數設定。
     * 有上傳圖片才重新編碼並更新圖片；未選圖時保留既有圖片，所有資料變更在同一交易內完成。
     * @param request 活動資料；column 是最後一排的字母標籤，row 是每排座位數
     * @param image 可省略的 JPEG 上傳檔
     * @return 儲存後的活動資料，包含座位配置及圖片 API 路徑
     */
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

    /**
     * 將最後一排標籤換算成排數，例如 Z＝26、AA＝27；字元格式由請求 DTO 預先驗證。
     * 演唱會、舞台劇及展覽的上限分別為 50、100、150 排，超過時回報 column 欄位錯誤。
     */
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

    /**
     * 將排號轉成英文字母標籤，支援超過 Z 的排號。
     */
    private String seatRowLabel(int number) {
        StringBuilder label = new StringBuilder();
        while (number > 0) {
            number--;
            label.insert(0, (char) ('A' + number % 26));
            number /= 26;
        }
        return label.toString();
    }

    /**
     * 檢查上傳的 MIME 與實際內容皆為 JPEG，檔案不可超過 10 MiB，來源最多 2400 萬像素。
     * 維持長寬比例且不放大，先限制最長邊為 1280；再嘗試降低品質或尺寸，使儲存內容不超過 1 MiB。
     * @param image 未上傳時可為 null
     * @return 重新編碼後的 JPEG 位元組及實際尺寸；未上傳則回傳 null
     * @throws FieldValidationException 格式、大小或解碼不符限制，或無法壓到儲存上限
     */
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

    /**
     * 以雙三次插值將圖片繪製成指定尺寸的 RGB 圖片；繪製後釋放 Graphics2D 資源。
     */
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

    /**
     * 依指定品質將圖片編碼為 JPEG 位元組；編碼失敗時拋出 IOException。
     */
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

    /**
     * 鎖定活動後確認是否存在及是否已有場次；不存在回傳 404，已有場次回傳 409 並附警告通知。
     * 允許刪除時，依序刪除圖片、活動座位配置與活動，回傳更新後的活動清單。
     * 警告推播失敗不會改變 409 回應，前端仍可使用回應內的通知內容。
     */
    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
    public ResponseEntity<?> deleteActivity(AdminDeleteActivityRequest request) {
        final String id = request.id().trim();
        if (adminMapper.lockActivity(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "活動不存在或已被刪除"));
        }
        if (adminMapper.activityHasSessions(id)) {
            String reason = "活動 " + id + " 已建立場次，無法刪除。請保留活動及相關訂單資料。";
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getPrincipal() instanceof LoginUser user
                    ? user.email() : authentication.getName();
            NotificationMessage notification = new NotificationMessage(
                    email,
                    "無法刪除活動", reason);
            notification.setId(UUID.randomUUID().toString());
            notification.setType("warning");
            try {
                notifierConsumer.sendNotification(notification);
            } catch (org.springframework.messaging.MessagingException ex) {
                org.slf4j.LoggerFactory.getLogger(AdminServiceImpl.class)
                        .warn("刪除活動警告推播失敗，改由 API 回應通知", ex);
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", reason, "notification", notification));
        }
        adminMapper.deleteActivityImage(id);
        adminMapper.delete_seat_by_activity(id);
        if (adminMapper.delete_activity(id) != 1) {
            throw new IllegalStateException("活動刪除失敗，請重新整理後再試");
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

    /**
     * id 空白時新增場次，已有 id 時更新日期、開賣時間及狀態，回傳完整場次清單。
     * 新場次容量由活動排數與每排座位數計算；已有分區設定的場次保留原容量，且不可更換活動。
     */
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
        Map<String, Object> configured = null;
        if (!id.isBlank()) {
            adminMapper.lockSession(id);
            configured = adminMapper.configuredSession(id);
            if (configured != null && !configured.isEmpty() && !activity_id.equals(configured.get("activity_id"))) {
                throw new FieldValidationException("activity_id", "已設定分區的場次不能更換活動");
            }
        }
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
        session.setCapacity(configured != null && !configured.isEmpty()
                ? new BigDecimal(configured.get("capacity").toString()) : capacity);
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

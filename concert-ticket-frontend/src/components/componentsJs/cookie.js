// 閱讀導引：封裝 cookie 的寫入、讀取及清除，供登入 token 與會員顯示名稱使用。
/**
 * 以 max-age=0 移除根路徑 cookie，供登出時清除前端登入資料。
 * @param {string} name cookie 名稱
 */
function clearCookie(name) {
    document.cookie = `${name}=; max-age=0; path=/; SameSite=Lax`;
}

/**
 * 將資料轉為 URI 編碼後寫入根路徑 cookie；物件先序列化為 JSON，HTTPS 時加上 Secure。
 * @param {string} name cookie 名稱
 * @param {*} value 要保存的值
 * @param {number|null} maxAge 有效秒數；null 表示瀏覽器工作階段 cookie
 */
function addCookie(name, value, maxAge = null) {
    const val =
        typeof value === "object"
            ? JSON.stringify(value)
            : value;
    let cookieStr = `${name}=${encodeURIComponent(val)}; path=/`;
    cookieStr += '; SameSite=Lax';
    if (window.location.protocol === 'https:') {
        cookieStr += '; Secure';
    }
    // 👇 有設定才變成「持久 cookie」
    if (maxAge !== null) {
        cookieStr += `; max-age=${maxAge}`;
    }
    document.cookie = cookieStr;
}

/**
 * 讀取 cookie 並還原 URI 編碼；可解析成 JSON 的內容會轉回原資料。
 * @param {string} name 要尋找的 cookie 名稱
 * @returns {*} JSON 值或字串；找不到時回傳 null
 */
function toFindCookie(name) {
    const cookies = document.cookie.split("; ");
        for (const cookie of cookies) {
            const index = cookie.indexOf("=");
            const key = cookie.substring(0, index);
            const value = cookie.substring(index + 1);
            if (key === name) {
                const decoded = decodeURIComponent(value);
                try {
                    return JSON.parse(decoded);
                } catch {
                    return decoded;
                }
            }
        }
        return null;
}

export {
    clearCookie, addCookie, toFindCookie
}

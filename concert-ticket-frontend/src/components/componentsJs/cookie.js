/**
 * <h3>清除cookie的方法</h3>
 * @param name 新增cookie的值
 */
function clearCookie(name) {
    document.cookie = `${name}=; max-age=0; path=/; SameSite=Lax`;
}

/**
 * <h3>新增cookie的方法</h3>
 * @param name 新增cookie的值
 * @param value 新增cookie的值
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
 * <h3>找到cookie的方法</h3>
 * @param name 新增cookie的值
 * @returns {string} cookie存token的值
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

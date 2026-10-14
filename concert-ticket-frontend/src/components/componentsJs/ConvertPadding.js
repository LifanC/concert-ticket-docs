// 閱讀導引：日期格式工具：將日期時間整理成畫面使用的格式。
/**
 * 取得現在時間，透過 Intl.DateTimeFormat 產生日期時間文字。
 * @param {boolean} data 是否使用 12 小時制，傳入格式器的 hour12 選項
 * @returns {string} 依瀏覽器時區及格式器地區設定產生的現在時間
 */
function dateConversionYMDhms(data) {
    let date = new Date();
    let formatter = new Intl.DateTimeFormat('tw', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: data,
    });

    return formatter.format(date);
}

export {
    dateConversionYMDhms
}

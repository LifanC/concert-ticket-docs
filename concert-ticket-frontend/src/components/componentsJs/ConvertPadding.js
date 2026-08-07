/**
 * <h3>ForMat日期YYYY-MM-DD HH:mm:ss</h3>
 * @param data 資料
 * @returns {String} 回傳資料前面補零
 * */
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
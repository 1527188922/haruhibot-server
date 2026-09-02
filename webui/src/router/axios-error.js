function pickMessage (res, fallbackMessage) {
  const data = res && res.data;
  if (data && typeof data === 'object') {
    return data.message || data.msg || fallbackMessage;
  }
  if (typeof data === 'string' && data.trim()) {
    return data;
  }
  return fallbackMessage;
}

function createHttpError (res, fallbackMessage, options) {
  const status = Number(res && res.status);
  const message = pickMessage(res, fallbackMessage || '请求失败');
  const error = new Error(message);
  error.status = status;
  error.response = res;
  if (options && options.handled) {
    error.handled = true;
  }
  return error;
}

module.exports = {
  createHttpError
};

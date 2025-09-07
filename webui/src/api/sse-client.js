
export default class SSEClient {
    /**
     * 创建SSE客户端实例
     * @param {Object} options - 配置选项
     * @param {string} options.url - SSE服务端URL
     * @param {Object} [options.params={}] - URL查询参数
     * @param {Object} [options.headers={}] - 请求头信息
     * @param {number} [options.reconnectDelay=3000] - 重连延迟时间(毫秒)
     * @param {number} [options.errorReconnectDelay=5000] - 错误重连延迟时间(毫秒)
     * @param {Function} [options.onMessage] - 收到消息的回调函数
     * @param {Function} [options.onOpen] - 连接成功的回调函数
     * @param {Function} [options.onError] - 发生错误的回调函数
     * @param {Function} [options.onClose] - 连接关闭的回调函数
     * @param {boolean} [options.autoReconnect=true] - 是否自动重连
     */
    constructor(options) {
        // 默认配置
        const defaultOptions = {
            url: '',
            params: {},
            headers: {},
            reconnectDelay: 3000,
            errorReconnectDelay: 5000,
            onMessage: () => {},
            onOpen: () => {},
            onError: () => {},
            onClose: () => {},
            autoReconnect: true
        };

        this.options = { ...defaultOptions, ...options };

        // 状态管理
        this.isClosed = false;
        this.isConnecting = false;
        this.reconnectTimer = null;
        this.reader = null;
        this.controller = null;
    }

    buildUrl() {
        if (!this.options.url) {
            throw new Error('SSE服务端URL不能为空');
        }

        const urlObj = new URL(this.options.url);
        const params = new URLSearchParams(this.options.params);

        // 添加所有查询参数
        for (const [key, value] of params) {
            urlObj.searchParams.append(key, value);
        }

        return urlObj.toString();
    }

    /**
     * 启动SSE连接
     */
    connect() {
        if (this.isConnecting || !this.options.autoReconnect && this.isClosed) {
            return;
        }

        this.isConnecting = true;
        this.isClosed = false;

        // 清除可能存在的重连定时器
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        // 创建AbortController用于手动终止请求
        this.controller = new AbortController();

        const url = this.buildUrl();

        fetch(url, {
            method: 'GET',
            keepalive:true,
            headers: this.options.headers,
            signal: this.controller.signal,
            // 跨域场景需要时开启（需后端配合CORS配置）
            // credentials: 'include'
        }).then(response => {
            this.isConnecting = false;

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            // 触发连接成功回调
            this.options.onOpen(response);

            // 处理响应流
            this.handleResponseStream(response);
        }).catch(error => {
            this.isConnecting = false;

            // 如果是主动中止的请求，不触发错误回调
            if (error.name !== 'AbortError') {
                this.options.onError(error);
                this.scheduleReconnect(true);
            }
        });
    }

    /**
     * 处理响应流
     * @param {Response} response - 响应对象
     */
    handleResponseStream(response) {
        if (!response.body) {
            throw new Error('响应不包含可读流');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');
        this.reader = reader;

        const processChunk = async () => {
            // 如果连接已关闭，停止处理
            if (this.isClosed) {
                return;
            }

            try {
                const { done, value } = await reader.read();

                if (done) {
                    // 流已结束
                    this.reader = null;

                    // 如果不是主动关闭，尝试重连
                    if (!this.isClosed) {
                        this.options.onClose('stream ended');
                        this.scheduleReconnect(false);
                    }
                    return;
                }

                // 解析SSE数据
                const chunk = decoder.decode(value, { stream: true });
                this.parseSSEData(chunk);

                // 继续处理下一个块
                processChunk();
            } catch (error) {
                this.reader = null;
                this.options.onError(error);

                if (!this.isClosed) {
                    this.scheduleReconnect(true);
                }
            }
        };

        // 开始处理流
        processChunk();
    }

    /**
     * 解析SSE格式的数据
     * @param {string} data - 原始数据
     */
    parseSSEData(data) {
        console.log(data)
        // SSE消息通常以"\n\n"分隔
        const lines = data.split('\n\n');

        lines.forEach(line => {
            if (!line || line.trim() === '') {
                return
            }
            // console.log(line)
            this.options.onMessage(line.trim());
        });
    }

    /**
     * 安排重连
     * @param {boolean} isError - 是否因错误导致的重连
     */
    scheduleReconnect(isError) {
        if (!this.options.autoReconnect || this.isClosed) {
            return;
        }

        const delay = isError
            ? this.options.errorReconnectDelay
            : this.options.reconnectDelay;

        this.reconnectTimer = setTimeout(() => {
            this.connect();
        }, delay);
    }

    /**
     * 更新配置参数
     * @param {Object} newOptions - 新的配置选项
     */
    updateOptions(newOptions) {
        this.options = { ...this.options, ...newOptions };

        // 如果已经连接，重启连接以应用新配置
        if (!this.isClosed) {
            this.close();
            this.connect();
        }
    }

    /**
     * 关闭SSE连接
     */
    close() {
        this.isClosed = true;

        // 中止请求
        if (this.controller) {
            this.controller.abort();
        }

        // 取消读取器
        if (this.reader) {
            this.reader.cancel();
        }

        // 清除重连定时器
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
        }

        this.isConnecting = false;
        this.options.onClose('manual close');
    }
}
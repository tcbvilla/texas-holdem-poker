/** Public-facing branding shown before login. */
export const PUBLIC_APP_NAME = '英语学习助手';
export const PUBLIC_TAGLINE = '词汇练习、学习笔记与进度管理';
export const PUBLIC_DESCRIPTION = '在线英语学习与笔记管理平台';

/** Internal branding shown after login. */
export const INTERNAL_APP_NAME = '德州扑克';

export function setDocumentTitle(title) {
    document.title = title;
}

export function setPublicTitle() {
    setDocumentTitle(PUBLIC_APP_NAME);
}

export function setInternalTitle(suffix) {
    setDocumentTitle(suffix ? `${INTERNAL_APP_NAME} - ${suffix}` : INTERNAL_APP_NAME);
}

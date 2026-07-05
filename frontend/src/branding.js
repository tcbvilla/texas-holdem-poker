/** Public-facing branding shown before login. */
export const PUBLIC_APP_NAME = '英语学习助手';
export const PUBLIC_TAGLINE = '词汇练习、学习笔记与进度管理';
export const PUBLIC_DESCRIPTION = '在线英语学习与笔记管理平台';

/** Internal branding shown after login (same learning theme). */
export const INTERNAL_APP_NAME = PUBLIC_APP_NAME;

/** User-facing terminology for logged-in pages (before entering a classroom/game). */
export const TERMS = {
  home: '首页',
  schoolMgmt: '学校管理',
  school: '学校',
  schools: '学校',
  mySchools: '我的学校',
  allSchools: '所有学校',
  createSchool: '创建学校',
  joinSchool: '加入学校',
  enterSchool: '进入学校',
  searchSchool: '搜索学校...',
  schoolName: '学校名称',
  schoolDesc: '学校描述',
  schoolNamePh: '请输入学校名称',
  schoolDescPh: '请输入学校描述',
  noSchoolJoined: '您还没有加入任何学校',
  noSchoolFound: '没有找到学校',
  selectSchoolFirst: '请先选择一个学校才能管理教室',
  schoolOnlyRooms: '教室只能在学校内创建和管理',

  classroomMgmt: '教室管理',
  classroom: '教室',
  classrooms: '教室',
  classroomList: '教室列表',
  createClassroom: '创建教室',
  enterClassroom: '进入教室',
  closeClassroom: '关闭教室',
  restartClassroom: '重新开课',
  confirmRestart: '确认重新开课',
  noClassrooms: '还没有创建任何教室',
  classroomName: '教室名称',
  classroomDesc: '教室描述',
  classroomNamePh: '请输入教室名称',
  classroomDescPh: '请输入教室描述',
  classroomCode: '教室编号',
  maxSeats: '最大人数',
  smallBookFee: '小书费',
  bigBookFee: '大书费',
  bookFee: '书费',
  defaultCredit: '初始学分',
  minPayment: '最低缴费',
  maxPayment: '最高缴费',
  duration: '课程时长(分钟)',
  actionTime: '互动时限(秒)',
  tuitionStats: '学费统计',
  totalPaid: '累计缴费',
  balance: '账户余额',
  tuitionDiff: '学费结余',
  student: '学员',
  noTuitionData: '暂无学费数据',
  restartHint: '可修改教室参数后重新开课，原有学费统计将被清除。',
  restartTitle: '重新开课',

  statusWaiting: '待开课',
  statusRunning: '上课中',
  statusFinished: '已结课',
  statusCancelled: '已停课',
};

export function setDocumentTitle(title) {
  document.title = title;
}

export function setPublicTitle() {
  setDocumentTitle(PUBLIC_APP_NAME);
}

export function setInternalTitle(suffix) {
  setDocumentTitle(suffix ? `${INTERNAL_APP_NAME} - ${suffix}` : INTERNAL_APP_NAME);
}

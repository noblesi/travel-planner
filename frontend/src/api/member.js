import http from "./http"

export async function getMemberInfo() { 
  const response = await http.get('/member/myPage')
  return response.data.data
}

export async function postModifyMemberInfo(requestData) { 
  console.log(requestData.memberName +"/"+requestData.email+"/"
    +requestData.genderCode+"/"+requestData.birthDate+"/"+requestData.phoneNumber+ "reqeust data")
  const response = await http.post('/member/modifyMemberInfo', requestData)
  return response.data.data
}

export async function getModifyNickname(nickname) { 
  console.log(nickname + "으로 변경 할거임")
  const response = await http.get('/member/modifyNickname', {params:{nickname}})
  return response.data.data
}

export async function postModifyProfileImage() { 
  const response = await http.post('/member/modifyProfileImage')
  return response.data.data
}

export async function getDeleteAccount() { 
  const response = await http.get('/member/deleteAccount')
  return response.data.data
}
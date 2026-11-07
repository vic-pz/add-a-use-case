<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item v-if="loaded" label="Status:">
      <el-tag :type="active ? 'success' : 'danger'">{{
        active ? 'Enabled' : 'Deactivated'
      }}</el-tag>
    </el-form-item>
    <el-form-item v-if="loaded && !active" label="Enable account:">
      <el-switch v-model="enableOnSave" aria-label="Enable account on save" />
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button
        v-if="loaded && active"
        type="danger"
        :loading="deactivating"
        @click="confirmDeactivation"
      >
        Deactivate Student
      </el-button>
      <el-button type="primary" :disabled="!loaded || deactivating" @click="updateExistingStudent">
        Confirm
      </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { isAxiosError } from 'axios'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
const emit = defineEmits(['close-dialog', 'status-changed'])

const loaded = ref(false)
const active = ref(false)
const enableOnSave = ref(false)
const deactivating = ref(false)

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  active.value = result.data.enabled === true
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
  loaded.value = true
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    enabled: active.value || enableOnSave.value,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  await updateStudent(updatedStudent, !active.value && enableOnSave.value)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}

async function confirmDeactivation() {
  if (deactivating.value) return
  deactivating.value = true
  try {
    await ElMessageBox.confirm(
      'This student will lose access, stop receiving reminders, and no longer be asked to submit work. Teammates will no longer be asked to rate this student. Their team membership, activity reports, and existing peer evaluations will be kept.',
      'Deactivate Student?',
      { type: 'warning', confirmButtonText: 'Deactivate', cancelButtonText: 'Cancel' }
    )
    await deactivateStudent(studentId)
    active.value = false
    emit('status-changed')
    ElMessage.success('Student deactivated.')
  } catch (error) {
    // The request interceptor displays API errors. A conflict means another request already succeeded.
    if (isAxiosError(error) && error.response?.status === 409) {
      active.value = false
      emit('status-changed')
    }
  } finally {
    deactivating.value = false
  }
}
</script>

<style scoped lang="scss"></style>

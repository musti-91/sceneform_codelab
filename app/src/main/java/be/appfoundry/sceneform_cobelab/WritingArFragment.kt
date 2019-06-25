package be.appfoundry.sceneform_cobelab

import com.google.ar.sceneform.ux.ArFragment


open class WritingArFragment : ArFragment() {
    override fun getAdditionalPermissions(): Array<String> {
        val additionalPermissions = super.getAdditionalPermissions()
        val permissionLength = if (additionalPermissions != null) additionalPermissions.size else 0
        val permissions = arrayOfNulls<String>(permissionLength + 1)
        permissions[0] = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (permissionLength > 0) {
            System.arraycopy(additionalPermissions, 0, permissions, 1, additionalPermissions.size)
        }

        return additionalPermissions
    }
}
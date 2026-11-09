
package br.com.icaros.fitcare.data

object SessaoUsuario {

    var usuarioAtual: UsuarioEntity? = null
        private set

    fun iniciar(usuario: UsuarioEntity) {
        usuarioAtual = usuario
    }

    fun encerrar() {
        usuarioAtual = null
    }

    fun ehProfessor(): Boolean {
        return usuarioAtual?.perfil == "PROFESSOR"
    }

    fun ehAluno(): Boolean {
        return usuarioAtual?.perfil == "ALUNO"
    }

    fun usuarioId(): Long? {
        return usuarioAtual?.id
    }
}

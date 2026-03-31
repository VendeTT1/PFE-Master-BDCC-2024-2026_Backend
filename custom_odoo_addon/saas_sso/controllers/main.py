from odoo import http,fields
from odoo.http import request
import jwt

SECRET = "my-super-secret-key-my-super-secret-key"

class SaasSSOController(http.Controller):

    @http.route('/saas-login', type='http', auth='public', csrf=False)
    def saas_login(self, token=None, **kwargs):

        if not token:
            return "Missing token"

        try:
            payload = jwt.decode(token, SECRET, algorithms=['HS256'])
            email = payload.get("sub")
            role = payload.get("role")

            if not email or not role:
                return "Invalid token"

            user = request.env['res.users'].sudo().search([
                ('login', '=', email)
            ], limit=1)

            internal_group = request.env.ref('base.group_user')
            admin_group = request.env.ref('base.group_system')

            if not user:
                if role == "OWNER":
                    groups = [internal_group.id, admin_group.id]
                else:
                    groups = [internal_group.id]

                user = request.env['res.users'].sudo().create({
                    'name': email,
                    'login': email,
                    'groups_id': [(6, 0, groups)]
                })

            user.sudo().write({
                'login_date': fields.Datetime.now()
            })

            request.session.logout()

            request.session.uid = user.id
            request.session.login = user.login
            request.session.session_token = user._compute_session_token(request.session.sid)

            return request.redirect('/web')

        except Exception as e:
            return f"SSO Error: {str(e)}"
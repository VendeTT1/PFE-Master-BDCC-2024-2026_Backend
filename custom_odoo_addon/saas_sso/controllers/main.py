from odoo import http
from odoo.http import request
from datetime import datetime
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

            user = request.env['res.users'].sudo().search([
                ('login', '=', email)
            ], limit=1)

            if not user:
                return "User not found"

            # ✅ Session
            request.session.uid = user.id
            request.session.login = user.login
            request.session.session_token = user._compute_session_token(request.session.sid)

            # ✅ Fix last login
            user.sudo().write({
                'login_date': datetime.now()
            })

            print("user logged in:",user.login_date)

            # ✅ Give admin rights (DEV MODE)
            admin_group = request.env.ref('base.group_system')
            user.sudo().write({
                'groups_id': [(6, 0, [admin_group.id])]
            })

            return request.redirect('/web')

        except Exception as e:
            return f"SSO Error: {str(e)}"

    @http.route('/saas-login', type='http', auth='public', csrf=False)
    def saas_login(self, token=None, **kwargs):

        if not token:
            return "Missing token"

        try:
            payload = jwt.decode(token, SECRET, algorithms=['HS256'])
            email = payload.get("sub")

            user = request.env['res.users'].sudo().search([
                ('login', '=', email)
            ], limit=1)

            if not user:
                return "User not found"

            # ✅ Proper session creation
            request.session.uid = user.id
            request.session.login = user.login
            request.session.session_token = user._compute_session_token(request.session.sid)

            return request.redirect('/web')

        except Exception as e:
            return f"SSO Error: {str(e)}"
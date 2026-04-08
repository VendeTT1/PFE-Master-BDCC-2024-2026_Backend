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

          # Validate that both email and role are present
            if not email or not role:
                return "Invalid token"

            # Search for the user by email
            user = request.env['res.users'].sudo().search([
                ('login', '=', email)
            ], limit=1)

            # If the user doesn't exist, return an error
            if not user:
                return "User not found"

            request.session.logout()

            request.session.uid = user.id
            request.session.login = user.login
            request.session.session_token = user._compute_session_token(request.session.sid)

            user.write({
                'login_date': fields.Datetime.now()
            })
            return request.redirect('/web')

        except Exception as e:
            return f"SSO Error: {str(e)}"
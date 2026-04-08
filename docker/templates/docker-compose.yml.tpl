services:
  ${INSTANCE_NAME}_db:
    image: postgres:15
    container_name: ${INSTANCE_NAME}_db
    environment:
      - POSTGRES_DB=${DB_NAME}
      - POSTGRES_USER=odoo
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - ${INSTANCE_NAME}_db_data:/var/lib/postgresql/data
    networks:
      - odoo_net_${INSTANCE_NAME}  # Added a space after the hyphen

  ${INSTANCE_NAME}_app:
    image: odoo:18
    container_name: ${INSTANCE_NAME}_app
    depends_on:
      - ${INSTANCE_NAME}_db
    environment:
      - HOST=${INSTANCE_NAME}_db
      - USER=odoo
      - PASSWORD=${DB_PASSWORD}
#      - script.py env
      - ODOO_URL=http://localhost:8069
      - ODOO_DB=${DB_NAME}
      - OLD_ADMIN_LOGIN=admin
      - OLD_ADMIN_PASSWORD=admin
      - NEW_ADMIN_LOGIN=${OWNER_EMAIL}
      - NEW_ADMIN_PASSWORD=
      - COMPANY_NAME=${INSTANCE_NAME}
      - COMPANY_PHONE=+212600000000
      - COMPANY_DOMAIN=https://${INSTANCE_NAME}.example.com
      - CLIENT_LOGIN=${OWNER_EMAIL}
      - CLIENT_PASSWORD=
    ports:
      - "${PORT}:8069"
    volumes:
      - ${INSTANCE_NAME}_odoo_data:/var/lib/odoo
      - ./odoo.conf:/etc/odoo/odoo.conf
      - C:/Users/VendeTTa/IdeaProjects/PFE-MASTER/custom_odoo_addon:/mnt/extra-addons
      - ./script.py:/script/script.py
    networks:
      - odoo_net_${INSTANCE_NAME}

networks:
  odoo_net_${INSTANCE_NAME}:
    name: odoo_net_${INSTANCE_NAME}
    driver: bridge

volumes:
  ${INSTANCE_NAME}_db_data:
    name: ${INSTANCE_NAME}_db_data
  ${INSTANCE_NAME}_odoo_data:
    name: ${INSTANCE_NAME}_odoo_data
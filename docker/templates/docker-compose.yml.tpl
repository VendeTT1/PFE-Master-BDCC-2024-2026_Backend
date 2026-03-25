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
    ports:
      - "${PORT}:8069"
    volumes:
      - ${INSTANCE_NAME}_odoo_data:/var/lib/odoo
      - ./odoo.conf:/etc/odoo/odoo.conf
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
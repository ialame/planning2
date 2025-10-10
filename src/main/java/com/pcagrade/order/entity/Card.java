package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚠️  ENTITÉ CARD - TABLE PARTAGÉE AVEC D'AUTRES PROJETS
 *
 * IMPORTANT : Cette table est utilisée par plusieurs projets.
 * - NE PAS faire de migrations automatiques
 * - NE PAS modifier la structure existante
 * - Utiliser seulement en LECTURE pour ce projet de planification
 */
@Getter
@Setter
@Immutable
@Entity
@Table(name = "card")
public class Card extends AbstractUlidEntity {

    @Size(max = 255)
    @NotNull
    @Column(name = "discriminator", nullable = false)
    private String discriminator;

    @Size(max = 255)
    @NotNull
    @Column(name = "num", nullable = false)
    private String num;

    /**
     * ✅ CORRECTIF : Mapping de la colonne existante SANS migration
     * - Utilise @Basic au lieu de @Lob pour éviter les tentatives de conversion
     * - Pas de columnDefinition pour éviter les conflits
     * - Hibernate prendra la définition existante de la base
     */
    @NotNull
    @Basic(fetch = FetchType.LAZY) // Chargement paresseux pour les gros attributs
    @Column(name = "attributes", nullable = false)
    private String attributes;

    /**
     * ✅ CORRECTIF : Mapping des notes existantes SANS migration
     */
    @NotNull
    @ColumnDefault("'[]'")
    @Basic(fetch = FetchType.LAZY) // Chargement paresseux
    @Column(name = "allowed_notes", nullable = false)
    private String allowedNotes;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "ap", nullable = false)
    private Boolean ap = false;

    @Column(name = "image_id")
    private Integer imageId;

    /**
     * ✅ Relation avec les traductions - LECTURE SEULE pour sécurité
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "card") // Pas de cascade pour éviter modifications
    private List<CardTranslation> translations = new ArrayList<>();

    // ===============================================
    // MÉTHODES UTILITAIRES POUR PLANIFICATION
    // ===============================================

    /**
     * Récupère le nom de la carte pour la planification (lecture seule)
     */
    public String getNomPourPlanification() {
        if (translations != null && !translations.isEmpty()) {
            return translations.get(0).getName();
        }
        return "Carte " + discriminator + " " + num;
    }

    /**
     * Vérifie si la carte a des attributs (pour estimation de temps)
     */
    public boolean hasComplexAttributes() {
        return attributes != null && attributes.length() > 100;
    }

    /**
     * Estimation du temps de traitement basé sur la complexité
     */
    public int getEstimatedProcessingMinutes() {
        int baseTime = 3; // 3 minutes par défaut

        if (hasComplexAttributes()) {
            baseTime += 2; // +2 minutes pour cartes complexes
        }

        if (translations != null && translations.size() > 1) {
            baseTime += 1; // +1 minute pour cartes multilingues
        }

        return baseTime;
    }
}